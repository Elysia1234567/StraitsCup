<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref, toRefs, withDefaults } from 'vue';

interface FlickeringGridProps {
  squareSize?: number;
  gridGap?: number;
  flickerChance?: number;
  color?: string;
  width?: number;
  height?: number;
  class?: string;
  maxOpacity?: number;
}

const props = withDefaults(defineProps<FlickeringGridProps>(), {
  squareSize: 4,
  gridGap: 6,
  flickerChance: 0.3,
  color: 'rgb(0, 0, 0)',
  maxOpacity: 0.3,
});

const { squareSize, gridGap, flickerChance, color, maxOpacity, width, height } = toRefs(props);

const containerRef = ref<HTMLElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const context = ref<CanvasRenderingContext2D | null>(null);

const isInView = ref(false);
const gridParams = ref<{ cols: number; rows: number; squares: Float32Array; dpr: number } | null>(null);

const computedColor = computed(() => {
  const c = color.value.trim();
  if (c.startsWith('#')) {
    const hex = c.replace('#', '');
    const fullHex = hex.length === 3 ? hex.split('').map((ch) => ch + ch).join('') : hex;
    const intVal = Number.parseInt(fullHex, 16);
    const r = (intVal >> 16) & 255;
    const g = (intVal >> 8) & 255;
    const b = intVal & 255;
    return `rgba(${r}, ${g}, ${b},`;
  }
  const nums = c.match(/\d+(\.\d+)?/g);
  if (nums && nums.length >= 3) {
    return `rgba(${nums[0]}, ${nums[1]}, ${nums[2]},`;
  }
  return 'rgba(255, 255, 255,';
});

function setupCanvas(canvas: HTMLCanvasElement, w: number, h: number) {
  const dpr = window.devicePixelRatio || 1;
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = `${w}px`;
  canvas.style.height = `${h}px`;

  const cols = Math.floor(w / (squareSize.value + gridGap.value));
  const rows = Math.floor(h / (squareSize.value + gridGap.value));
  const squares = new Float32Array(cols * rows);

  for (let i = 0; i < squares.length; i++) {
    squares[i] = Math.random() * maxOpacity.value;
  }
  return { cols, rows, squares, dpr };
}

function updateSquares(squares: Float32Array, deltaTime: number) {
  for (let i = 0; i < squares.length; i++) {
    if (Math.random() < flickerChance.value * deltaTime) {
      squares[i] = Math.random() * maxOpacity.value;
    }
  }
}

function drawGrid(
  ctx: CanvasRenderingContext2D,
  widthPx: number,
  heightPx: number,
  cols: number,
  rows: number,
  squares: Float32Array,
  dpr: number,
) {
  ctx.clearRect(0, 0, widthPx, heightPx);
  for (let i = 0; i < cols; i++) {
    for (let j = 0; j < rows; j++) {
      const opacity = squares[i * rows + j];
      ctx.fillStyle = `${computedColor.value}${opacity})`;
      ctx.fillRect(
        i * (squareSize.value + gridGap.value) * dpr,
        j * (squareSize.value + gridGap.value) * dpr,
        squareSize.value * dpr,
        squareSize.value * dpr,
      );
    }
  }
}

function updateCanvasSize() {
  if (!canvasRef.value || !containerRef.value) return;
  const w = width.value ?? containerRef.value.clientWidth;
  const h = height.value ?? containerRef.value.clientHeight;
  gridParams.value = setupCanvas(canvasRef.value, w, h);
}

let animationFrameId: number | undefined;
let resizeObserver: ResizeObserver | undefined;
let intersectionObserver: IntersectionObserver | undefined;
let lastTime = 0;

function animate(time: number) {
  if (!isInView.value || !context.value || !canvasRef.value || !gridParams.value) return;
  const deltaTime = (time - lastTime) / 1000;
  lastTime = time;

  updateSquares(gridParams.value.squares, deltaTime);
  drawGrid(
    context.value,
    canvasRef.value.width,
    canvasRef.value.height,
    gridParams.value.cols,
    gridParams.value.rows,
    gridParams.value.squares,
    gridParams.value.dpr,
  );
  animationFrameId = requestAnimationFrame(animate);
}

onMounted(() => {
  if (!canvasRef.value || !containerRef.value) return;
  context.value = canvasRef.value.getContext('2d');
  if (!context.value) return;

  updateCanvasSize();

  resizeObserver = new ResizeObserver(() => {
    updateCanvasSize();
  });

  intersectionObserver = new IntersectionObserver(
    ([entry]) => {
      isInView.value = entry.isIntersecting;
      if (isInView.value) {
        animationFrameId = requestAnimationFrame(animate);
      }
    },
    { threshold: 0 },
  );

  resizeObserver.observe(containerRef.value);
  intersectionObserver.observe(canvasRef.value);
});

onBeforeUnmount(() => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId);
  resizeObserver?.disconnect();
  intersectionObserver?.disconnect();
});
</script>

<template>
  <div
    ref="containerRef"
    :class="props.class"
  >
    <canvas
      ref="canvasRef"
      class="pointer-events-none block size-full"
    />
  </div>
</template>
