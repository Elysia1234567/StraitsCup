<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any  */

import { Camera, Mesh, Plane, Program, Raycast, Renderer, Texture, Transform } from "ogl";
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

/** 底部画廊点击仅传出图片地址 */
export type GalleryCardPickPayload = {
  image: string;
  title: string;
  description: string;
};

type GL = Renderer["gl"];
type GalleryItem = {
  image: string;
  text: string;
  title: string;
  description: string;
};

interface CircularGalleryProps {
  items?: GalleryItem[];
  /** 地图区域对应组索引，将整组旋到视口中心（组内对齐中间一张卡） */
  focusGroupIndex?: number | null;
  bend?: number;
  textColor?: string;
  borderRadius?: number;
  font?: string;
}

const emit = defineEmits<{
  cardPick: [payload: GalleryCardPickPayload];
}>();

const props = withDefaults(defineProps<CircularGalleryProps>(), {
  bend: 25,
  textColor: "#ffffff",
  borderRadius: 0.05,
  font: "bold 22px DM Sans",
  focusGroupIndex: null,
});

const containerRef = ref<HTMLDivElement | null>(null);
let app: App | null = null;

function getMaxGroupIndex(): number {
  const itemCount = props.items?.length ?? 0;
  if (itemCount <= 0) return 0;
  return Math.max(0, Math.floor((itemCount - 1) / 3));
}

function tryApplyFocusGroup() {
  const g = props.focusGroupIndex;
  if (app == null || g == null || typeof g !== "number") return;
  const maxGroupIndex = getMaxGroupIndex();
  if (!(g >= 0) || !(g <= maxGroupIndex)) return;
  app.scrollToGroup(g, maxGroupIndex);
}

function debounce<T extends (...args: any[]) => void>(func: T, wait: number) {
  let timeout: number;
  return function (this: any, ...args: Parameters<T>) {
    window.clearTimeout(timeout);
    timeout = window.setTimeout(() => func.apply(this, args), wait);
  };
}

function lerp(p1: number, p2: number, t: number): number {
  return p1 + (p2 - p1) * t;
}

function autoBind(instance: any): void {
  const proto = Object.getPrototypeOf(instance);
  Object.getOwnPropertyNames(proto).forEach((key) => {
    if (key !== "constructor" && typeof instance[key] === "function") {
      instance[key] = instance[key].bind(instance);
    }
  });
}

function getFontSize(font: string): number {
  const match = font.match(/(\d+)px/);
  return match ? Number.parseInt(match[1], 10) : 30;
}

function createTextTexture(
  gl: GL,
  text: string,
  font: string = "bold 30px monospace",
  color: string = "black",
): { texture: Texture; width: number; height: number } {
  const canvas = document.createElement("canvas");
  const context = canvas.getContext("2d");
  if (!context) throw new Error("Could not get 2d context");

  context.font = font;
  const metrics = context.measureText(text);
  const textWidth = Math.ceil(metrics.width);
  const fontSize = getFontSize(font);
  const textHeight = Math.ceil(fontSize * 1.2);

  canvas.width = textWidth + 20;
  canvas.height = textHeight + 20;

  context.font = font;
  context.fillStyle = color;
  context.textBaseline = "middle";
  context.textAlign = "center";
  context.clearRect(0, 0, canvas.width, canvas.height);
  context.fillText(text, canvas.width / 2, canvas.height / 2);

  const texture = new Texture(gl, { generateMipmaps: false });
  texture.image = canvas;
  return { texture, width: canvas.width, height: canvas.height };
}

interface TitleProps {
  gl: GL;
  plane: Mesh;
  renderer: Renderer;
  text: string;
  textColor?: string;
  font?: string;
}

class Title {
  gl: GL;
  plane: Mesh;
  renderer: Renderer;
  text: string;
  textColor: string;
  font: string;
  mesh!: Mesh;

  constructor({
    gl,
    plane,
    renderer,
    text,
    textColor = "#545050",
    font = "30px sans-serif",
  }: TitleProps) {
    autoBind(this);
    this.gl = gl;
    this.plane = plane;
    this.renderer = renderer;
    this.text = text;
    this.textColor = textColor;
    this.font = font;
    this.createMesh();
  }

  createMesh() {
    const { texture, width, height } = createTextTexture(
      this.gl,
      this.text,
      this.font,
      this.textColor,
    );
    const geometry = new Plane(this.gl);
    const program = new Program(this.gl, {
      vertex: `
          attribute vec3 position;
          attribute vec2 uv;
          uniform mat4 modelViewMatrix;
          uniform mat4 projectionMatrix;
          varying vec2 vUv;
          void main() {
            vUv = uv;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
          }
        `,
      fragment: `
          precision highp float;
          uniform sampler2D tMap;
          varying vec2 vUv;
          void main() {
            vec4 color = texture2D(tMap, vUv);
            if (color.a < 0.1) discard;
            gl_FragColor = color;
          }
        `,
      uniforms: { tMap: { value: texture } },
      transparent: true,
    });
    this.mesh = new Mesh(this.gl, { geometry, program });
    const aspect = width / height;
    const textHeightScaled = this.plane.scale.y * 0.17;
    const textWidthScaled = textHeightScaled * aspect;
    this.mesh.scale.set(textWidthScaled, textHeightScaled, 1);
    this.mesh.position.y = this.plane.scale.y * 0.5 + textHeightScaled * 0.5 + 0.01;
    this.mesh.setParent(this.plane);
  }
}

interface ScreenSize {
  width: number;
  height: number;
}

interface Viewport {
  width: number;
  height: number;
}

interface MediaProps {
  geometry: Plane;
  gl: GL;
  image: string;
  index: number;
  length: number;
  renderer: Renderer;
  scene: Transform;
  screen: ScreenSize;
  text: string;
  viewport: Viewport;
  bend: number;
  textColor: string;
  borderRadius?: number;
  font?: string;
}

class Media {
  extra: number = 0;
  geometry: Plane;
  gl: GL;
  image: string;
  index: number;
  length: number;
  renderer: Renderer;
  scene: Transform;
  screen: ScreenSize;
  text: string;
  viewport: Viewport;
  bend: number;
  textColor: string;
  borderRadius: number;
  font?: string;
  program!: Program;
  plane!: Mesh;
  title!: Title;
  padding!: number;
  width!: number;
  widthTotal!: number;
  x!: number;
  speed: number = 0;
  isBefore: boolean = false;
  isAfter: boolean = false;

  constructor({
    geometry,
    gl,
    image,
    index,
    length,
    renderer,
    scene,
    screen,
    text,
    viewport,
    bend,
    textColor,
    borderRadius = 0,
    font,
  }: MediaProps) {
    this.geometry = geometry;
    this.gl = gl;
    this.image = image;
    this.index = index;
    this.length = length;
    this.renderer = renderer;
    this.scene = scene;
    this.screen = screen;
    this.text = text;
    this.viewport = viewport;
    this.bend = bend;
    this.textColor = textColor;
    this.borderRadius = borderRadius;
    this.font = font;
    this.createShader();
    this.createMesh();
    this.createTitle();
    this.onResize();
  }

  createShader() {
    const texture = new Texture(this.gl, { generateMipmaps: false });
    this.program = new Program(this.gl, {
      depthTest: false,
      depthWrite: false,
      vertex: `
          precision highp float;
          attribute vec3 position;
          attribute vec2 uv;
          uniform mat4 modelViewMatrix;
          uniform mat4 projectionMatrix;
          uniform float uTime;
          uniform float uSpeed;
          varying vec2 vUv;
          void main() {
            vUv = uv;
            vec3 p = position;
            p.z = (sin(p.x * 4.0 + uTime) * 1.5 + cos(p.y * 2.0 + uTime) * 1.5) * (0.1 + uSpeed * 0.5);
            gl_Position = projectionMatrix * modelViewMatrix * vec4(p, 1.0);
          }
        `,
      fragment: `
          precision highp float;
          uniform sampler2D tMap;
          uniform float uBorderRadius;
          varying vec2 vUv;
          float roundedBoxSDF(vec2 p, vec2 b, float r) {
            vec2 d = abs(p) - b;
            return length(max(d, vec2(0.0))) + min(max(d.x, d.y), 0.0) - r;
          }
          void main() {
            vec4 color = texture2D(tMap, vUv);
            float d = roundedBoxSDF(vUv - 0.5, vec2(0.5 - uBorderRadius), uBorderRadius);
            if(d > 0.0) {
              discard;
            }
            gl_FragColor = vec4(color.rgb, 1.0);
          }
        `,
      uniforms: {
        tMap: { value: texture },
        uSpeed: { value: 0 },
        uTime: { value: 100 * Math.random() },
        uBorderRadius: { value: this.borderRadius },
      },
      transparent: true,
    });
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = this.image;
    img.onload = () => {
      texture.image = img;
    };
  }

  createMesh() {
    this.plane = new Mesh(this.gl, {
      geometry: this.geometry,
      program: this.program,
    });
    this.plane.setParent(this.scene);
  }

  createTitle() {
    this.title = new Title({
      gl: this.gl,
      plane: this.plane,
      renderer: this.renderer,
      text: this.text,
      textColor: this.textColor,
      font: this.font,
    });
  }

  update(scroll: { current: number; last: number }, direction: "right" | "left") {
    this.plane.position.x = this.x - scroll.current - this.extra;

    const x = this.plane.position.x;
    const h = this.viewport.width / 2;

    if (this.bend === 0) {
      this.plane.position.y = 0;
      this.plane.rotation.z = 0;
    } else {
      const bAbs = Math.abs(this.bend);
      const r = (h * h + bAbs * bAbs) / (2 * bAbs);
      const effectiveX = Math.min(Math.abs(x), h);

      const arc = r - Math.sqrt(r * r - effectiveX * effectiveX);
      if (this.bend > 0) {
        this.plane.position.y = -arc;
        this.plane.rotation.z = -Math.sign(x) * Math.asin(effectiveX / r);
      } else {
        this.plane.position.y = arc;
        this.plane.rotation.z = Math.sign(x) * Math.asin(effectiveX / r);
      }
    }

    this.speed = scroll.current - scroll.last;
    this.program.uniforms.uTime.value += 0.04;
    this.program.uniforms.uSpeed.value = this.speed;

    const planeOffset = this.plane.scale.x / 2;
    const viewportOffset = this.viewport.width / 2;
    this.isBefore = this.plane.position.x + planeOffset < -viewportOffset;
    this.isAfter = this.plane.position.x - planeOffset > viewportOffset;
    if (direction === "right" && this.isBefore) {
      this.extra -= this.widthTotal;
      this.isBefore = this.isAfter = false;
    }
    if (direction === "left" && this.isAfter) {
      this.extra += this.widthTotal;
      this.isBefore = this.isAfter = false;
    }
  }

  onResize({ screen, viewport }: { screen?: ScreenSize; viewport?: Viewport } = {}) {
    if (screen) this.screen = screen;
    if (viewport) this.viewport = viewport;
    const pageH = typeof window !== 'undefined' ? window.innerHeight : this.screen.height;
    /** 单张卡片目标高度：页面高度的 1/5（与相机视锥、画布高度的比例关系一致） */
    const cardHeightPx = pageH * 0.2;
    this.plane.scale.y = (cardHeightPx * this.viewport.height) / this.screen.height;
    const cardAspect = 700 / 900;
    const cardWidthPx = cardHeightPx * cardAspect;
    this.plane.scale.x = (cardWidthPx * this.viewport.width) / this.screen.width;
    /** 相邻卡片之间的空隙（世界单位），约为卡片宽度的 38%，随卡片尺寸缩放 */
    this.padding = Math.max(2.2, this.plane.scale.x * 0.4);
    this.width = this.plane.scale.x + this.padding;
    this.widthTotal = this.width * this.length;
    this.x = this.width * this.index;
  }
}

interface AppConfig {
  items?: GalleryItem[];
  bend?: number;
  textColor?: string;
  borderRadius?: number;
  font?: string;
  onCardPick?: (payload: GalleryCardPickPayload) => void;
}

const DRAG_THRESHOLD_PX = 8;

class App {
  container: HTMLElement;
  canvas!: HTMLCanvasElement;
  raycaster: Raycast = new Raycast();
  onCardPick?: (payload: GalleryCardPickPayload) => void;
  canvasPointerSession = false;
  lastPointerX = 0;
  lastPointerY = 0;
  downPointerX = 0;
  downPointerY = 0;
  hasDragged = false;
  scroll: {
    ease: number;
    current: number;
    target: number;
    last: number;
    position?: number;
  };
  onCheckDebounce: (...args: any[]) => void;
  renderer!: Renderer;
  gl!: GL;
  camera!: Camera;
  scene!: Transform;
  planeGeometry!: Plane;
  medias: Media[] = [];
  mediasImages: GalleryItem[] = [];
  screen!: { width: number; height: number };
  viewport!: { width: number; height: number };
  raf: number = 0;

  boundOnResize!: () => void;
  boundOnWheel!: () => void;
  boundOnTouchDown!: (e: MouseEvent | TouchEvent) => void;
  boundOnTouchMove!: (e: MouseEvent | TouchEvent) => void;
  boundOnTouchUp!: () => void;

  isDown: boolean = false;
  start: number = 0;

  constructor(
    container: HTMLElement,
    {
      items,
      bend = 1,
      textColor = "#ffffff",
      borderRadius = 0,
      font = "bold 22px DM Sans",
      onCardPick,
    }: AppConfig,
  ) {
    this.container = container;
    this.onCardPick = onCardPick;
    this.scroll = { ease: 0.05, current: 0, target: 0, last: 0 };
    this.onCheckDebounce = debounce(this.onCheck.bind(this), 200);
    this.createRenderer();
    this.createCamera();
    this.createScene();
    this.onResize();
    this.createGeometry();
    this.createMedias(items, bend, textColor, borderRadius, font);
    this.update();
    this.addEventListeners();
  }

  createRenderer() {
    this.renderer = new Renderer({ alpha: true });
    this.gl = this.renderer.gl;
    this.gl.clearColor(0, 0, 0, 0);
    this.canvas = this.renderer.gl.canvas as HTMLCanvasElement;
    this.container.appendChild(this.canvas);
  }

  createCamera() {
    this.camera = new Camera(this.gl);
    this.camera.fov = 45;
    this.camera.position.z = 20;
  }

  createScene() {
    this.scene = new Transform();
  }

  createGeometry() {
    this.planeGeometry = new Plane(this.gl, {
      heightSegments: 50,
      widthSegments: 100,
    });
  }

  createMedias(
    items: GalleryItem[] | undefined,
    bend: number = 1,
    textColor: string,
    borderRadius: number,
    font: string,
  ) {
    const defaultItems = [
      {
        image: "https://picsum.photos/seed/1/800/600?grayscale",
        text: "Bridge",
        title: "Bridge",
        description: "A bridge landscape card.",
      },
      {
        image: "https://picsum.photos/seed/2/800/600?grayscale",
        text: "Desk Setup",
        title: "Desk Setup",
        description: "A desk setup lifestyle card.",
      },
      {
        image: "https://picsum.photos/seed/3/800/600?grayscale",
        text: "Waterfall",
        title: "Waterfall",
        description: "A waterfall scene card.",
      },
      {
        image: "https://picsum.photos/seed/4/800/600?grayscale",
        text: "Strawberries",
        title: "Strawberries",
        description: "A strawberries detail card.",
      },
      {
        image: "https://picsum.photos/seed/5/800/600?grayscale",
        text: "Deep Diving",
        title: "Deep Diving",
        description: "A deep diving atmosphere card.",
      },
      {
        image: "https://picsum.photos/seed/16/800/600?grayscale",
        text: "Train Track",
        title: "Train Track",
        description: "A train track travel card.",
      },
      {
        image: "https://picsum.photos/seed/17/800/600?grayscale",
        text: "Santorini",
        title: "Santorini",
        description: "A Santorini cityscape card.",
      },
      {
        image: "https://picsum.photos/seed/8/800/600?grayscale",
        text: "Blurry Lights",
        title: "Blurry Lights",
        description: "A blurry lights visual card.",
      },
      {
        image: "https://picsum.photos/seed/9/800/600?grayscale",
        text: "New York",
        title: "New York",
        description: "A New York skyline card.",
      },
      {
        image: "https://picsum.photos/seed/10/800/600?grayscale",
        text: "Good Boy",
        title: "Good Boy",
        description: "A portrait style card.",
      },
      {
        image: "https://picsum.photos/seed/21/800/600?grayscale",
        text: "Coastline",
        title: "Coastline",
        description: "A coastline nature card.",
      },
      {
        image: "https://picsum.photos/seed/12/800/600?grayscale",
        text: "Palm Trees",
        title: "Palm Trees",
        description: "A palm trees tropical card.",
      },
    ];
    const galleryItems = items && items.length ? items : defaultItems;
    /** 传入 12 条时仍双份循环；传入 24 条（如 8 组×3）则不再拼接 */
    this.mediasImages =
      galleryItems.length === 12 ? galleryItems.concat(galleryItems) : galleryItems;
    this.medias = this.mediasImages.map((data, index) => {
      return new Media({
        geometry: this.planeGeometry,
        gl: this.gl,
        image: data.image,
        index,
        length: this.mediasImages.length,
        renderer: this.renderer,
        scene: this.scene,
        screen: this.screen,
        text: data.text,
        viewport: this.viewport,
        bend,
        textColor,
        borderRadius,
        font,
      });
    });
  }

  onTouchDown(e: MouseEvent | TouchEvent) {
    if (!this.isPointerOnCanvas(e)) return;
    this.canvasPointerSession = true;
    this.isDown = true;
    this.hasDragged = false;
    const { x, y } = this.getClientXY(e);
    this.downPointerX = x;
    this.downPointerY = y;
    this.lastPointerX = x;
    this.lastPointerY = y;
    this.scroll.position = this.scroll.current;
    this.start = x;
  }

  onTouchMove(e: MouseEvent | TouchEvent) {
    if (!this.isDown) return;
    const { x, y } = this.getClientXY(e);
    this.lastPointerX = x;
    this.lastPointerY = y;
    const dx = x - this.downPointerX;
    const dy = y - this.downPointerY;
    if (dx * dx + dy * dy > DRAG_THRESHOLD_PX * DRAG_THRESHOLD_PX) {
      this.hasDragged = true;
    }
    const distance = (this.start - x) * 0.05;
    this.scroll.target = (this.scroll.position ?? 0) + distance;
  }

  onTouchUp() {
    if (this.canvasPointerSession) {
      this.canvasPointerSession = false;
      if (this.isDown && !this.hasDragged) {
        this.pickCard(this.lastPointerX, this.lastPointerY);
      }
    }
    this.isDown = false;
    this.hasDragged = false;
    this.onCheck();
  }

  onWheel() {
    this.scroll.target += 2;
    this.onCheckDebounce();
  }

  onCheck() {
    if (!this.medias || !this.medias[0]) return;
    const width = this.medias[0].width;
    const itemIndex = Math.round(Math.abs(this.scroll.target) / width);
    const item = width * itemIndex;
    this.scroll.target = this.scroll.target < 0 ? -item : item;
  }

  /** 将第 groupIndex 组（每组 3 张）的中间一张对齐到视口水平中心 */
  scrollToGroup(groupIndex: number, maxGroupIndex?: number) {
    if (!this.medias?.[0]) return;
    const width = this.medias[0].width;
    if (!(width > 0)) return;
    const computedMax =
      maxGroupIndex != null
        ? maxGroupIndex
        : Math.max(0, Math.floor((this.mediasImages.length - 1) / 3));
    const g = Math.max(0, Math.min(computedMax, Math.floor(groupIndex)));
    const middleCardIndex = g * 3 + 1;
    this.scroll.target = middleCardIndex * width;
    this.onCheck();
  }

  getClientXY(e: MouseEvent | TouchEvent): { x: number; y: number } {
    if ("touches" in e && e.touches.length > 0) {
      return { x: e.touches[0].clientX, y: e.touches[0].clientY };
    }
    const me = e as MouseEvent;
    return { x: me.clientX, y: me.clientY };
  }

  isPointerOnCanvas(e: MouseEvent | TouchEvent): boolean {
    const { x, y } = this.getClientXY(e);
    const r = this.canvas.getBoundingClientRect();
    return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;
  }

  pickCard(clientX: number, clientY: number) {
    if (!this.onCardPick || !this.medias?.length) return;
    this.scene.updateMatrixWorld();
    this.camera.updateMatrixWorld();
    const rect = this.canvas.getBoundingClientRect();
    if (!(rect.width > 0) || !(rect.height > 0)) return;
    const ndcX = ((clientX - rect.left) / rect.width) * 2 - 1;
    const ndcY = -((clientY - rect.top) / rect.height) * 2 + 1;
    this.raycaster.castMouse(this.camera, [ndcX, ndcY]);
    const meshes = this.medias.map((m) => m.plane);
    const hits = this.raycaster.intersectMeshes(meshes, { cullFace: false });
    const mesh = hits[0];
    if (!mesh) return;
    const media = this.medias.find((m) => m.plane === mesh);
    if (!media) return;
    const data = this.mediasImages[media.index];
    if (!data) return;
    this.onCardPick({ image: data.image, title: data.title, description: data.description });
  }

  onResize() {
    this.screen = {
      width: this.container.clientWidth,
      height: this.container.clientHeight,
    };
    this.renderer.setSize(this.screen.width, this.screen.height);
    this.camera.perspective({
      aspect: this.screen.width / this.screen.height,
    });
    const fov = (this.camera.fov * Math.PI) / 180;
    const height = 2 * Math.tan(fov / 2) * this.camera.position.z;
    const width = height * this.camera.aspect;
    this.viewport = { width, height };
    if (this.medias) {
      this.medias.forEach((media) =>
        media.onResize({ screen: this.screen, viewport: this.viewport }),
      );
    }
  }

  update() {
    this.scroll.current = lerp(this.scroll.current, this.scroll.target, this.scroll.ease);
    const direction = this.scroll.current > this.scroll.last ? "right" : "left";
    if (this.medias) {
      this.medias.forEach((media) => media.update(this.scroll, direction));
    }
    this.renderer.render({ scene: this.scene, camera: this.camera });
    this.scroll.last = this.scroll.current;
    this.raf = window.requestAnimationFrame(this.update.bind(this));
  }

  addEventListeners() {
    this.boundOnResize = this.onResize.bind(this);
    this.boundOnWheel = this.onWheel.bind(this);
    this.boundOnTouchDown = this.onTouchDown.bind(this);
    this.boundOnTouchMove = this.onTouchMove.bind(this);
    this.boundOnTouchUp = this.onTouchUp.bind(this);
    window.addEventListener("resize", this.boundOnResize);
    window.addEventListener("mousewheel", this.boundOnWheel);
    window.addEventListener("wheel", this.boundOnWheel);
    window.addEventListener("mousedown", this.boundOnTouchDown);
    window.addEventListener("mousemove", this.boundOnTouchMove);
    window.addEventListener("mouseup", this.boundOnTouchUp);
    window.addEventListener("touchstart", this.boundOnTouchDown);
    window.addEventListener("touchmove", this.boundOnTouchMove);
    window.addEventListener("touchend", this.boundOnTouchUp);
  }

  destroy() {
    window.cancelAnimationFrame(this.raf);
    window.removeEventListener("resize", this.boundOnResize);
    window.removeEventListener("mousewheel", this.boundOnWheel);
    window.removeEventListener("wheel", this.boundOnWheel);
    window.removeEventListener("mousedown", this.boundOnTouchDown);
    window.removeEventListener("mousemove", this.boundOnTouchMove);
    window.removeEventListener("mouseup", this.boundOnTouchUp);
    window.removeEventListener("touchstart", this.boundOnTouchDown);
    window.removeEventListener("touchmove", this.boundOnTouchMove);
    window.removeEventListener("touchend", this.boundOnTouchUp);
    if (this.renderer && this.renderer.gl && this.renderer.gl.canvas.parentNode) {
      this.renderer.gl.canvas.parentNode.removeChild(this.renderer.gl.canvas as HTMLCanvasElement);
    }
  }
}

function getAppInitConfig(): AppConfig {
  return {
    items: props.items,
    bend: props.bend,
    textColor: props.textColor,
    borderRadius: props.borderRadius,
    font: props.font,
    onCardPick: (payload) => emit("cardPick", payload),
  };
}

onMounted(() => {
  if (!containerRef.value) return;

  app = new App(containerRef.value, getAppInitConfig());
  void nextTick(() => tryApplyFocusGroup());
});

watch([() => props.bend, () => props.textColor, () => props.borderRadius, () => props.font], () => {
  if (app) {
    app.destroy();
    app = null;
  }

  if (containerRef.value) {
    app = new App(containerRef.value, getAppInitConfig());
    void nextTick(() => tryApplyFocusGroup());
  }
});

watch(
  () => props.items,
  () => {
    if (app) {
      app.destroy();
      app = null;
    }

    if (containerRef.value) {
      app = new App(containerRef.value, getAppInitConfig());
      void nextTick(() => tryApplyFocusGroup());
    }
  },
  { deep: true },
);

watch(
  () => props.focusGroupIndex,
  () => {
    tryApplyFocusGroup();
  },
);

onBeforeUnmount(() => {
  if (app) {
    app.destroy();
    app = null;
  }
});
</script>

<template>
  <div
    ref="containerRef"
    class="h-full w-full cursor-grab overflow-hidden active:cursor-grabbing"
  />
</template>
