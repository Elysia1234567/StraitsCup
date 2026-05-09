<script setup lang="ts">
import BlackHoleBackground from "@/components/background/BlackHoleBackground.vue";
import GraphPageReplicaCustomNode from "@/components/graph/GraphPageReplicaCustomNode.vue";
import { buildGraphPageReplicaJson } from "@/data/graphPageReplicaData";
import { RGMiniView, RelationGraph } from "relation-graph/vue3";
import type { RGLine, RGLink, RGNode, RGOptions, RGUserEvent } from "relation-graph/vue3";
import { nextTick, onMounted, onUnmounted, ref } from "vue";
import "@/components/graph/graph-page-replica.css";

const graphRef = ref<InstanceType<typeof RelationGraph> | null>(null);
const graphShellRef = ref<HTMLElement | null>(null);

/** 力导向布局：连线牵引 + 节点斥力；展开/收起后由 `reLayoutWhenExpandedOrCollapsed` 触发重算 */
const graphOptions: RGOptions = {
  debug: false,
  backgroundColor: "transparent",
  backgroundImage: "",
  canvasOpacity: 1,
  defaultLineShape: 1,
  defaultNodeShape: 0,
  defaultNodeWidth: 108,
  defaultNodeHeight: 108,
  defaultLineTextOnPath: true,
  /** 赛博朋克：连线 / 标签 / 展开钮基色（细节仍由 graph-page-replica.css 强化） */
  defaultLineColor: "rgba(34, 211, 238, 0.78)",
  defaultLineFontColor: "#cffafe",
  defaultLineWidth: 2.5,
  checkedLineColor: "#f472b6",
  defaultNodeFontColor: "#cbd5e1",
  defaultExpandHolderColor: "#22d3ee",
  defaultNodeBorderWidth: 0,
  layout: {
    layoutName: "force",
    fastStart: true,
    maxLayoutTimes: 420,
    byNode: true,
    byLine: true,
    force_node_repulsion: 2.2,
    force_line_elastic: 2.35,
  },
  defaultExpandHolderPosition: "right",
  reLayoutWhenExpandedOrCollapsed: true,
};

let resizeTimer: ReturnType<typeof setTimeout> | undefined;
let resizeObserver: ResizeObserver | undefined;

async function initializeGraph() {
  const rg = graphRef.value;
  if (!rg) return;
  const json = buildGraphPageReplicaJson();
  await rg.setJsonData(json, (inst) => {
    inst.moveToCenter();
    inst.zoomToFit();
  });
}

function onNodeClick(node: RGNode, _e: RGUserEvent) {
  console.log("onNodeClick:", node.text);
  return true;
}

function onLineClick(line: RGLine, _link: RGLink, _e: RGUserEvent) {
  console.log("onLineClick:", line.text, line.from, line.to);
  return true;
}

onMounted(async () => {
  await nextTick();
  await nextTick();
  requestAnimationFrame(() => {
    void initializeGraph();
  });

  if (graphShellRef.value) {
    resizeObserver = new ResizeObserver(() => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(() => {
        graphRef.value?.onGraphResize();
      }, 80);
    });
    resizeObserver.observe(graphShellRef.value);
  }
});

onUnmounted(() => {
  resizeObserver?.disconnect();
  clearTimeout(resizeTimer);
});
</script>

<template>
  <!--
    占位一层视口高度，避免「只有 fixed 子元素」时 main 高度塌成 0 导致整页滚动、背景与内容竖排错觉。
    黑洞：cover + fixed（组件内），最底层 z-[1]。
    图谱+标题：同一套 fixed 全屏叠在上面 z-[15]（低于顶栏 z-30）。
    图谱实现复刻 `graph/index.vue` + `graph/MyGraph.vue`（relation-graph/vue3 等价 API），节点内容由 `GraphPageReplicaCustomNode` 渲染（城市名 + 画廊图）。
  -->
  <main
    class="relative h-[100dvh] max-h-[100dvh] min-h-0 w-full shrink-0 flex-1 overflow-hidden bg-black text-white"
  >
    <BlackHoleBackground cover />

    <div
      class="pointer-events-none fixed inset-0 z-[15] flex max-h-[100dvh] flex-col pt-16 [padding-bottom:env(safe-area-inset-bottom,0px)]"
    >
      <section
        class="pointer-events-auto relative min-h-0 flex-1 overflow-hidden px-1 pt-0 pb-0 sm:px-3"
      >
        <div ref="graphShellRef" class="my-graph absolute inset-0 min-h-[120px]">
          <RelationGraph
            ref="graphRef"
            class="h-full min-h-0 w-full [&_.relation-graph]:!bg-transparent"
            :options="graphOptions"
            :on-node-click="onNodeClick"
            :on-line-click="onLineClick"
          >
            <template #graph-plug>
              <RGMiniView position="br" width="200px" height="120px" />
            </template>
            <template #node="{ node }">
              <GraphPageReplicaCustomNode :node="node" />
            </template>
          </RelationGraph>
        </div>
      </section>
      <footer
        class="pointer-events-auto shrink-0 px-4 pb-4 pt-2 text-center [text-shadow:0_0_24px_rgba(34,211,238,0.35)]"
      >
        <h1
          class="bg-gradient-to-r from-cyan-200 via-fuchsia-200 to-cyan-300 bg-clip-text text-2xl font-semibold tracking-[0.32em] text-transparent sm:text-3xl"
        >
          FUJIAN HERITAGE GRAPH
        </h1>
      </footer>
    </div>
  </main>
</template>
