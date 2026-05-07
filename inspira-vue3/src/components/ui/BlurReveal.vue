<script setup lang="ts">
import { Motion } from "motion-v";
import type { HTMLAttributes } from "vue";
import { cn } from "@inspira-ui/plugins";

interface BlurRevealProps {
  delay?: number;
  duration?: number;
  class?: HTMLAttributes["class"];
}

const props = withDefaults(defineProps<BlurRevealProps>(), {
  delay: 0.2,
  duration: 0.75,
});

const initialVariants = {
  opacity: 0,
  y: 12,
  filter: "blur(10px)",
};

const visibleVariants = {
  opacity: 1,
  y: 0,
  filter: "blur(0px)",
};
</script>

<template>
  <Motion
    :class="cn($props.class)"
    :initial="initialVariants"
    :while-in-view="visibleVariants"
    :transition="{
      delay: props.delay,
      duration: props.duration,
      ease: 'easeOut',
    }"
  >
    <slot />
  </Motion>
</template>
