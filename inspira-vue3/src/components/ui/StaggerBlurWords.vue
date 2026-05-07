<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { cn } from "@inspira-ui/plugins";
import { computed, onMounted, ref } from "vue";

const props = withDefaults(
  defineProps<{
    words: string;
    filter?: boolean;
    duration?: number;
    delay?: number;
    /** 相邻词开始显现的间隔（毫秒） */
    wordStaggerMs?: number;
    class?: HTMLAttributes["class"];
  }>(),
  { duration: 0.7, delay: 0, filter: true, wordStaggerMs: 200 },
);

const scope = ref<HTMLElement | null>(null);
const wordsArray = computed(() => props.words.split(/\s+/).filter(Boolean));

const spanStyle = computed(() => ({
  opacity: 0,
  filter: props.filter ? "blur(10px)" : "none",
  transition: `opacity ${props.duration}s, filter ${props.duration}s`,
}));

onMounted(() => {
  const root = scope.value;
  if (!root) return;

  const spans = root.querySelectorAll("span");

  window.setTimeout(() => {
    spans.forEach((span, index) => {
      window.setTimeout(() => {
        const el = span as HTMLElement;
        el.style.opacity = "1";
        el.style.filter = props.filter ? "blur(0px)" : "none";
      }, index * props.wordStaggerMs);
    });
  }, props.delay);
});
</script>

<template>
  <div :class="cn(`leading-snug tracking-wide`, props.class)">
    <div ref="scope">
      <span
        v-for="(word, idx) in wordsArray"
        :key="word + idx"
        class="inline-block"
        :style="spanStyle"
      >
        {{ word }}&nbsp;
      </span>
    </div>
  </div>
</template>
