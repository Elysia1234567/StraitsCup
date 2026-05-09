<script setup lang="ts">
import { buildFujianCultureGraphJson, FUJIAN_GRAPH_ROOT_ID } from "@/data/fujianCultureGraph";
import RelationGraph from "relation-graph/vue3";
import type { RGNode, RGOptions } from "relation-graph/vue3";
import { nextTick, onMounted, onUnmounted, ref } from "vue";

const graphRef = ref<InstanceType<typeof RelationGraph> | null>(null);
const shellRef = ref<HTMLElement | null>(null);

/**
 * 与参考 `graph/MyGraph.vue` 一致：center 布局 + 展开/收起时重算位置；
 * 右侧加减号控制展开（`defaultExpandHolderPosition: right`）。
 * 勿同时设置 layout 与 layouts。
 */
const graphOptions: RGOptions = {
  backgroundColor: "transparent",
  backgroundImage: "",
  canvasOpacity: 1,
  disableDragCanvas: false,
  disableZoom: false,
  moveToCenterWhenRefresh: false,
  zoomToFitWhenRefresh: false,
  useAnimationWhenRefresh: false,
  useAnimationWhenExpanded: false,
  reLayoutWhenExpandedOrCollapsed: true,
  performanceMode: false,
  allowShowMiniToolBar: false,
  allowShowRefreshButton: false,
  allowShowDownloadButton: false,
  allowSwitchLineShape: false,
  allowSwitchJunctionPoint: false,
  defaultShowLineLabel: true,
  defaultNodeShape: 1,
  defaultLineShape: 4,
  defaultLineWidth: 1,
  defaultNodeBorderWidth: 1,
  defaultFocusRootNode: true,
  defaultExpandHolderPosition: "right",
  defaultExpandHolderColor: "rgba(34, 211, 238, 0.95)",
  defaultNodeFontColor: "#e2e8f0",
  defaultLineFontColor: "#94a3b8",
  defaultLineColor: "rgba(34, 211, 238, 0.65)",
  checkedLineColor: "#f472b6",
  minCanvasZoom: 5,
  hideNodeContentByZoom: false,
  layout: {
    layoutName: "center",
    maxLayoutTimes: 3000,
  },
};

/**
 * 将视图对准福建省根节点（relation-graph：focusRootNode / handleSelect 会把该节点移到视口中心）
 */
async function centerOnFujianRoot() {
  const rg = graphRef.value;
  const inst = rg?.getInstance();
  if (!rg || !inst) return;
  rg.onGraphResize();
  try {
    await inst.focusRootNode();
  } catch {
    await inst.focusNodeById(FUJIAN_GRAPH_ROOT_ID);
  }
}

/** 收起时把所有后代节点的 expanded 关掉，下次再展开父节点时只会露出紧邻下一层 */
function collapseDescendantsExpandedFlag(n: RGNode) {
  n.lot?.childs?.forEach((c) => {
    c.expanded = false;
    collapseDescendantsExpandedFlag(c);
  });
}

/** 用右侧加减号展开/收起；收起时同步子树 expanded，避免再次展开时状态错乱 */
function onGraphNodeCollapse(node: RGNode) {
  collapseDescendantsExpandedFlag(node);
}

function applyGraphData() {
  const json = buildFujianCultureGraphJson();
  const rg = graphRef.value;
  if (!rg) return;
  rg.setJsonData(json, () => {
    const tick = () => {
      void centerOnFujianRoot();
    };
    tick();
    setTimeout(tick, 120);
    setTimeout(tick, 450);
    setTimeout(tick, 1000);
  });
}

let resizeTimer: ReturnType<typeof setTimeout> | undefined;
let resizeObserver: ResizeObserver | undefined;

function scheduleResizeReflow() {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => {
    void centerOnFujianRoot();
  }, 80);
}

onMounted(async () => {
  await nextTick();
  await nextTick();
  requestAnimationFrame(() => {
    applyGraphData();
  });

  if (shellRef.value) {
    resizeObserver = new ResizeObserver(() => {
      scheduleResizeReflow();
    });
    resizeObserver.observe(shellRef.value);
  }

  window.addEventListener("resize", scheduleResizeReflow);
});

onUnmounted(() => {
  resizeObserver?.disconnect();
  window.removeEventListener("resize", scheduleResizeReflow);
  clearTimeout(resizeTimer);
});
</script>

<template>
  <div ref="shellRef" class="fujian-relation-graph cyber-graph-shell relative z-10 h-full min-h-0 w-full">
    <RelationGraph
      ref="graphRef"
      class="graph-rg-host h-full min-h-0 w-full"
      :options="graphOptions"
      :on-node-collapse="onGraphNodeCollapse"
    />
  </div>
</template>

<style scoped>
.cyber-graph-shell {
  box-sizing: border-box;
}

/* 图谱区域完全透明，仅保留节点/连线；禁止模糊滤镜以免背景发糊 */
.cyber-graph-shell :deep(.relation-graph) {
  height: 100% !important;
  min-height: 0 !important;
  background: transparent !important;
}

.cyber-graph-shell :deep(.rel-background),
.cyber-graph-shell :deep(.rel-map) {
  height: 100% !important;
  min-height: 0 !important;
  background: transparent !important;
  opacity: 1 !important;
}

.cyber-graph-shell :deep(.rel-map svg) {
  background: transparent !important;
}

.cyber-graph-shell :deep(.fj-graph-node.rel-diy-node) {
  padding: 0;
  overflow: visible;
}

/* 去掉 relation-graph 外层矩形/圆形底色（inline style），仅保留内层椭圆 pill */
.cyber-graph-shell :deep(.fj-graph-node--pill-host.rel-diy-node) {
  background-color: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.cyber-graph-shell :deep(.fj-ng) {
  box-sizing: border-box;
  font-family:
    ui-sans-serif,
    system-ui,
    "Segoe UI",
    sans-serif;
}

.cyber-graph-shell :deep(.fj-ng-root-wrap),
.cyber-graph-shell :deep(.fj-ng-city-wrap) {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.cyber-graph-shell :deep(.fj-ng-root-pill) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px 26px;
  border-radius: 9999px;
  background: radial-gradient(circle at 30% 25%, rgba(34, 211, 238, 0.35), transparent 55%),
    rgba(8, 15, 35, 0.82);
  border: 2px solid rgba(34, 211, 238, 0.85);
  box-shadow:
    inset 0 0 18px rgba(34, 211, 238, 0.2),
    0 0 12px rgba(34, 211, 238, 0.28);
}

.cyber-graph-shell :deep(.fj-ng-root-title) {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #ecfeff;
}

.cyber-graph-shell :deep(.fj-ng-root-sub) {
  font-size: 9px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(165, 243, 252, 0.85);
}

.cyber-graph-shell :deep(.fj-ng-city-pill) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 20px;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #fff7ed;
  background: linear-gradient(145deg, rgba(251, 146, 60, 0.28), rgba(15, 10, 25, 0.88));
  border: 1px solid rgba(251, 146, 60, 0.55);
  box-shadow:
    inset 0 0 12px rgba(251, 146, 60, 0.12),
    0 0 10px rgba(251, 146, 60, 0.22);
}

.cyber-graph-shell :deep(.fj-ng-culture-wrap) {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.cyber-graph-shell :deep(.fj-ng-culture-card) {
  display: flex;
  width: 100%;
  max-width: 100%;
  flex-direction: column;
  align-items: stretch;
  padding: 6px;
  gap: 5px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(52, 211, 153, 0.18), rgba(6, 12, 28, 0.9));
  border: 1px solid rgba(52, 211, 153, 0.45);
  box-shadow:
    inset 0 0 12px rgba(52, 211, 153, 0.08),
    0 0 12px rgba(52, 211, 153, 0.15);
}

.cyber-graph-shell :deep(.fj-ng-culture-img) {
  width: 100%;
  height: 72px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid rgba(52, 211, 153, 0.4);
}

.cyber-graph-shell :deep(.fj-ng-culture-title) {
  font-size: 10px;
  line-height: 1.25;
  text-align: center;
  color: #ecfccb;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cyber-graph-shell :deep(.fj-ng-leaf-wrap) {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.cyber-graph-shell :deep(.fj-ng-leaf-pill) {
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
  border-radius: 9999px;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.22), rgba(10, 12, 28, 0.9));
  border: 1px solid rgba(192, 132, 252, 0.45);
  box-shadow:
    inset 0 0 12px rgba(168, 85, 247, 0.08),
    0 0 10px rgba(168, 85, 247, 0.12);
}

.cyber-graph-shell :deep(.fj-ng-leaf-k) {
  font-size: 9px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #e9d5ff;
}

.cyber-graph-shell :deep(.fj-ng-leaf-v) {
  font-size: 10px;
  line-height: 1.35;
  color: rgba(241, 245, 249, 0.92);
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
