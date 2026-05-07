<script lang="ts" setup>
import { cn } from "@inspira-ui/plugins";
import { ref } from "vue";

interface Props {
  text?: string;
  class?: string;
}

withDefaults(defineProps<Props>(), {
  text: "Button",
});

defineEmits<{
  click: [event: MouseEvent];
}>();

const buttonRef = ref<HTMLButtonElement>();
</script>

<template>
  <button
    ref="buttonRef"
    :class="
      cn(
        `group relative w-auto cursor-pointer overflow-hidden rounded-full border border-violet-300/40 bg-slate-950/90 p-2 px-6 text-center font-semibold text-white shadow-[0_0_0_1px_rgba(167,139,250,0.15)] transition-all duration-300 hover:border-white hover:bg-violet-600`,
        $props.class,
      )
    "
    type="button"
    @click="$emit('click', $event)"
  >
    <div class="flex items-center gap-2">
      <div
        class="size-2 rounded-lg bg-white shadow-[0_0_14px_rgba(255,255,255,0.95)] transition-all duration-300 group-hover:scale-[40] group-hover:bg-white group-hover:shadow-[0_0_34px_rgba(255,255,255,1)]"
      />
      <span
        class="inline-block whitespace-nowrap transition-all duration-300 group-hover:translate-x-14 group-hover:opacity-0"
      >
        {{ $props.text }}
      </span>
    </div>

    <div
      class="absolute top-0 z-10 flex size-full translate-x-14 items-center justify-center gap-2 font-bold text-slate-950 opacity-0 transition-all duration-300 group-hover:-translate-x-6 group-hover:opacity-100"
    >
      <span class="whitespace-nowrap">{{ $props.text }}</span>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="lucide lucide-arrow-right transition-transform duration-300 group-hover:translate-x-1"
      >
        <path d="M5 12h14" />
        <path d="m12 5 7 7-7 7" />
      </svg>
    </div>
  </button>
</template>
