<script lang="ts" setup>
import { onMounted, onBeforeUnmount, ref } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { OBJLoader } from 'three/examples/jsm/loaders/OBJLoader.js';
import { MTLLoader } from 'three/examples/jsm/loaders/MTLLoader.js';

const canvasRef = ref<HTMLCanvasElement | null>(null);
const loadingMsg = ref('加载中...');
const hasError = ref(false);

let renderer: THREE.WebGLRenderer;
let animId: number;

onMounted(() => {
  const canvas = canvasRef.value!;
  const w = canvas.clientWidth;
  const h = canvas.clientHeight;

  // 场景 / 相机
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x0f172a);

  const camera = new THREE.PerspectiveCamera(45, w / h, 0.01, 1000);
  camera.position.set(0, 2.5, 5);

  // 渲染器
  renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(w, h);
  renderer.shadowMap.enabled = true;

  // 灯光
  scene.add(new THREE.AmbientLight(0xffffff, 0.6));
  const dir = new THREE.DirectionalLight(0xffffff, 1.2);
  dir.position.set(5, 10, 7);
  dir.castShadow = true;
  scene.add(dir);

  // 地面网格辅助线
  scene.add(new THREE.GridHelper(10, 20, 0x334155, 0x1e293b));

  // 轨道控制器
  const controls = new OrbitControls(camera, canvas);
  controls.enableDamping = true;
  controls.dampingFactor = 0.05;
  controls.minDistance = 0.5;
  controls.maxDistance = 50;

  // 按优先级依次尝试加载，全部失败则显示占位体
  // glb/gltf 用 GLTFLoader；obj 用 OBJLoader（自动尝试同名 .mtl 材质）
  const MODEL_CANDIDATES = [
    '/models/170516_mia337_032122_600_200Kfaces_8192px_OBJ.gltf',
    '/models/scene.glb',
    '/models/scene.obj',
  ];
  const gltfLoader = new GLTFLoader();
  const objLoader = new OBJLoader();
  const mtlLoader = new MTLLoader();

  function fitToScene(model: THREE.Object3D) {
    // 先缩放
    const box0 = new THREE.Box3().setFromObject(model);
    const size = box0.getSize(new THREE.Vector3());
    const maxDim = Math.max(size.x, size.y, size.z) || 1;
    const scale = 2 / maxDim;
    model.scale.setScalar(scale);

    // 重新计算缩放后的包围盒
    const box1 = new THREE.Box3().setFromObject(model);
    const center = box1.getCenter(new THREE.Vector3());

    // X/Z 居中，Y 让底部贴地（y=0）
    model.position.x -= center.x;
    model.position.z -= center.z;
    model.position.y -= box1.min.y;   // 底部对齐 y=0

    scene.add(model);
    loadingMsg.value = '';
  }

  function showFallback() {
    loadingMsg.value = '';
    hasError.value = true;
    const geo = new THREE.TorusKnotGeometry(0.8, 0.25, 128, 32);
    const mat = new THREE.MeshStandardMaterial({ color: 0x7c3aed, roughness: 0.3, metalness: 0.6 });
    scene.add(new THREE.Mesh(geo, mat));
  }

  function onProgress(xhr: ProgressEvent) {
    const pct = Math.round((xhr.loaded / (xhr.total || 1)) * 100);
    loadingMsg.value = `加载中 ${pct}%`;
  }

  function tryLoad(index: number) {
    if (index >= MODEL_CANDIDATES.length) { showFallback(); return; }
    const url = MODEL_CANDIDATES[index];

    if (url.endsWith('.obj')) {
      // 先尝试加载同名 .mtl，无论成功与否都继续加载 obj
      const mtlUrl = url.replace('.obj', '.mtl');
      mtlLoader.load(
        mtlUrl,
        (mtl) => {
          mtl.preload();
          objLoader.setMaterials(mtl);
          objLoader.load(url, fitToScene, onProgress, () => tryLoad(index + 1));
        },
        undefined,
        // mtl 不存在时直接加载 obj（无材质）
        () => objLoader.load(url, fitToScene, onProgress, () => tryLoad(index + 1)),
      );
    } else {
      gltfLoader.load(
        url,
        (gltf) => fitToScene(gltf.scene),
        onProgress,
        () => tryLoad(index + 1),
      );
    }
  }

  tryLoad(0);

  // 响应式尺寸
  const onResize = () => {
    const w2 = canvas.clientWidth;
    const h2 = canvas.clientHeight;
    camera.aspect = w2 / h2;
    camera.updateProjectionMatrix();
    renderer.setSize(w2, h2);
  };
  window.addEventListener('resize', onResize);

  // 动画循环
  const clock = new THREE.Clock();
  const animate = () => {
    animId = requestAnimationFrame(animate);
    controls.update();
    // 让场景中第一个 Mesh 缓慢自转（模型加载前的占位体也适用）
    const mesh = scene.children.find((c) => c instanceof THREE.Mesh) as THREE.Mesh | undefined;
    if (mesh) mesh.rotation.y += clock.getDelta() * 0.4;
    renderer.render(scene, camera);
  };
  animate();

  onBeforeUnmount(() => {
    cancelAnimationFrame(animId);
    renderer.dispose();
    window.removeEventListener('resize', onResize);
  });
});
</script>

<template>
  <div class="relative h-screen w-full bg-slate-950">
    <!-- 顶部标题栏 -->
    <div class="absolute top-0 left-0 z-10 flex items-center gap-3 px-6 py-4">
      <router-link to="/infinite-grid" class="text-white/50 hover:text-white transition-colors text-sm">← 返回</router-link>
      <span class="text-white/20">|</span>
      <h1 class="text-white font-semibold tracking-wide">3D 模型展示</h1>
    </div>

    <!-- 加载提示 -->
    <div
      v-if="loadingMsg"
      class="absolute inset-0 z-10 flex items-center justify-center text-white/60 text-sm pointer-events-none"
    >
      {{ loadingMsg }}
    </div>

    <!-- 占位提示 -->
    <div
      v-if="hasError"
      class="absolute bottom-6 left-1/2 -translate-x-1/2 z-10 rounded-lg bg-white/5 px-4 py-2 text-white/50 text-xs pointer-events-none"
    >
      未找到模型文件，显示占位几何体。将模型放到 public/models/ 目录，支持 scene.glb / scene.gltf / scene.obj（可附带同名 .mtl）。
    </div>

    <!-- Three.js canvas -->
    <canvas ref="canvasRef" class="h-full w-full" />
  </div>
</template>
