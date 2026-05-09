<template>
  <!-- 根：霓虹描边 + 内层暗色 HUD -->
  <div
    v-if="node.id === rootId"
    class="cyber-root-ring relative z-[555] flex h-full w-full place-items-center justify-center overflow-hidden rounded-full p-[6px]"
  >
    <div
      class="cyber-root-inner flex h-full w-full flex-col items-center justify-center overflow-hidden rounded-full px-6 py-4 text-center"
    >
      <span class="text-[32px] font-bold tracking-[0.32em] text-cyan-100 [text-shadow:0_0_18px_rgba(34,211,238,0.85)]">{{
        node.text
      }}</span>
      <span
        class="mt-2 text-[18px] tracking-[0.55em] text-fuchsia-300/90 [text-shadow:0_0_12px_rgba(232,121,254,0.65)]"
        >HERITAGE NET</span
      >
    </div>
  </div>

  <!-- 第 2 级：冷色 · 青碧霓虹（行政区枢纽） -->
  <div
    v-else-if="isTier2"
    class="graph-tier2-node relative flex h-full w-full items-center justify-center overflow-hidden rounded-full px-4"
  >
    <div
      class="pointer-events-none absolute inset-0 rounded-full bg-gradient-to-br from-cyan-500/25 via-transparent to-transparent opacity-90"
      aria-hidden="true"
    />
    <span
      class="relative text-center text-[30px] font-bold leading-snug tracking-[0.12em] text-cyan-100 [text-shadow:0_0_14px_rgba(34,211,238,0.75)]"
      >{{ node.text }}</span
    >
  </div>

  <!-- 第 4 级：暖撞色 · 品红底 + 酸性黄字（数据叶节点），与 2 级明显区分 -->
  <div
    v-else-if="isLeafTier"
    class="graph-tier4-node relative flex h-full w-full items-center justify-center overflow-hidden rounded-full px-2"
  >
    <div
      class="pointer-events-none absolute inset-0 rounded-full bg-gradient-to-tl from-amber-400/10 via-transparent to-fuchsia-500/20 opacity-95"
      aria-hidden="true"
    />
    <span
      class="relative max-w-full text-center text-[20px] font-semibold leading-[1.15] tracking-wide text-amber-200 [text-shadow:0_0_8px_rgba(253,224,71,0.5)] sm:text-[16px]"
      >{{ node.text }}</span
    >
  </div>

  <!-- 第 3 级：纯圆图（无描边/外发光）+ 仅标题条 -->
  <div
    v-else
    class="graph-tier3-host relative flex h-full w-full place-items-center justify-center overflow-visible rounded-full bg-transparent"
  >
    <img
      v-if="cityImage"
      :src="cityImage"
      :alt="node.text"
      class="h-[min(90%,104px)] w-[min(90%,104px)] shrink-0 rounded-full object-cover shadow-none ring-0 outline-none"
      loading="lazy"
      @error="imageFailed = true"
    />
    <PhotoIcon v-else class="h-16 w-16 text-cyan-300/90" aria-hidden="true" />

    <div
      class="my-node-text cyber-tier3-caption absolute max-w-[min(96%,336px)] rounded-md border border-cyan-400/45 bg-[rgba(6,10,22,0.92)] px-3 py-1 text-center backdrop-blur-sm"
      :style="labelStyleTier3"
    >
      <span
        class="block text-[20px] font-semibold leading-snug tracking-wide text-cyan-100 [text-shadow:0_0_8px_rgba(34,211,238,0.45)] sm:text-[22px]"
        >{{ node.text }}</span
      >
    </div>
  </div>
</template>

<script setup lang="ts">
import { PhotoIcon } from "@heroicons/vue/24/outline";
import { computed, ref } from "vue";
import type { RGNode } from "relation-graph/vue3";
import { GRAPH_PAGE_REPLICA_ROOT_ID } from "@/data/graphPageReplicaData";
import "./graph-page-replica-node-animation.css";

const props = defineProps<{
  node: RGNode;
}>();

const rootId = GRAPH_PAGE_REPLICA_ROOT_ID;

const imageFailed = ref(false);

const cityImage = computed(() => {
  if (imageFailed.value) return "";
  const url = props.node.data?.cityImage;
  return typeof url === "string" ? url : "";
});

const isTier2 = computed(() => props.node.data?.tier === 2);
const isLeafTier = computed(() => props.node.data?.tier === 4);

const labelStyleTier3 = {
  marginTop: "100%",
  transform: "translateY(24px)",
};
</script>
