<script setup lang="ts">
import { cn } from "@inspira-ui/plugins";
import { computed } from "vue";

interface FlipCardProps {
  rotate?: "x" | "y";
  class?: string;
}

const props = withDefaults(defineProps<FlipCardProps>(), {
  rotate: "y",
});

/**
 * 必须在旋转父级上设置 preserve-3d，否则正背面会被压成 2D，后绘制的背面盖住正面。
 * 使用 hover: 仅在指针位于本卡片上时翻面，避免与其它组件的 `group` / group-hover 串扰。
 */
const rotationClass = {
  x: ["hover:[transform:rotateX(180deg)]", "[transform:rotateX(180deg)]"],
  y: ["hover:[transform:rotateY(180deg)]", "[transform:rotateY(180deg)]"],
};

const rotation = computed(() => rotationClass[props.rotate]);

const frontIdentityTransform = computed(() =>
  props.rotate === "x" ? "[transform:rotateX(0deg)]" : "[transform:rotateY(0deg)]",
);
</script>

<template>
  <div :class="cn(`relative h-72 w-56 [perspective:1000px]`, props.class)">
    <div
      :class="
        cn(
          `relative h-full rounded-2xl transition-transform duration-500 [transform-style:preserve-3d] will-change-transform`,
          rotation[0],
        )
      "
    >
      <!-- Front -->
      <div
        :class="
          cn(
            `absolute inset-0 size-full overflow-hidden rounded-2xl border backface-hidden [backface-visibility:hidden]`,
            frontIdentityTransform,
          )
        "
      >
        <slot />
      </div>

      <!-- Back -->
      <div
        :class="
          cn(
            `absolute inset-0 h-full w-full overflow-hidden rounded-2xl border bg-black/80 p-4 text-slate-200 [backface-visibility:hidden]`,
            rotation[1],
          )
        "
      >
        <slot name="back" />
      </div>
    </div>
  </div>
</template>
