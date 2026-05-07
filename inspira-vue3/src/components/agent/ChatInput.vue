<template>
  <div class="mx-auto w-full max-w-3xl">
    <div class="relative rounded-2xl border border-white/25 bg-transparent text-white shadow-sm backdrop-blur-sm">
      <textarea
        ref="textareaRef"
        v-model="inputText"
        rows="1"
        class="block w-full resize-none overflow-hidden bg-transparent px-3 pb-9 pt-2.5 text-base leading-6 text-white outline-none placeholder:text-white/60 disabled:opacity-50"
        :placeholder="placeholder"
        :disabled="disabled"
        @input="adjustTextareaHeight"
        @keydown.enter.prevent="handleSend"
      />

      <div class="absolute bottom-0 left-0 right-0 flex items-center justify-between border-t border-white/15 px-1.5 py-0.5">
        <div class="flex min-w-0 flex-1 items-center gap-0.5 overflow-x-auto">
          <button class="shrink-0 rounded-md p-1 transition hover:bg-white/10" type="button">
            <PlusIcon class="h-4 w-4 text-white/80" />
          </button>
          <button class="flex shrink-0 items-center gap-0.5 rounded-md px-1.5 py-0.5 transition hover:bg-white/10" type="button">
            <SparklesIcon class="h-3.5 w-3.5 text-white/80" />
            <span class="text-xs text-white/80">任务助理</span>
          </button>
          <button class="flex shrink-0 items-center gap-0.5 rounded-md px-1.5 py-0.5 transition hover:bg-white/10" type="button">
            <LightBulbIcon class="h-3.5 w-3.5 text-white/80" />
            <span class="text-xs text-white/80">深度思考</span>
          </button>
          <button
            class="flex shrink-0 items-center gap-0.5 rounded-md px-1.5 py-0.5 transition hover:bg-white/10"
            :class="{ 'bg-white/15 ring-1 ring-white/20': searchEnabled }"
            type="button"
            :disabled="disabled"
            @click="searchEnabled = !searchEnabled"
          >
            <MagnifyingGlassIcon class="h-3.5 w-3.5 text-white/80" />
            <span class="text-xs text-white/80">联网搜索</span>
          </button>
          <button class="flex shrink-0 items-center gap-0.5 rounded-md px-1.5 py-0.5 transition hover:bg-white/10" type="button">
            <PhotoIcon class="h-3.5 w-3.5 text-white/80" />
            <span class="text-xs text-white/80">AI生图</span>
          </button>
          <button class="flex shrink-0 items-center gap-0.5 rounded-md px-1.5 py-0.5 transition hover:bg-white/10" type="button">
            <PresentationChartBarIcon class="h-3.5 w-3.5 text-white/80" />
            <span class="text-xs text-white/80">PPT创作</span>
          </button>
          <button class="flex shrink-0 items-center gap-0.5 rounded-md px-1.5 py-0.5 transition hover:bg-white/10" type="button">
            <span class="text-xs text-white/80">更多</span>
            <ChevronDownIcon class="h-3.5 w-3.5 text-white/80" />
          </button>
        </div>

        <div class="flex shrink-0 items-center gap-1 pl-1">
          <button class="rounded-md p-1 transition hover:bg-white/10" type="button">
            <MicrophoneIcon class="h-4 w-4 text-white/80" />
          </button>
          <button
            class="rounded-md bg-primary p-1 text-white transition hover:bg-primary/90 disabled:opacity-50"
            type="button"
            :disabled="disabled || !inputText.trim()"
            @click="handleSend"
          >
            <ArrowUpIcon class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue';
import {
  PlusIcon,
  SparklesIcon,
  LightBulbIcon,
  MagnifyingGlassIcon,
  PhotoIcon,
  PresentationChartBarIcon,
  ChevronDownIcon,
  MicrophoneIcon,
  ArrowUpIcon,
} from '@heroicons/vue/24/outline';

const inputText = ref('');
const searchEnabled = ref(false);
const textareaRef = ref(null);

/** 文本区最多展示约 4 行（与原先整体约 152px 可视区一致的量级） */
const MAX_TEXT_LINES = 4;

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: '向千问提问',
  },
});

const emit = defineEmits(['send']);

function adjustTextareaHeight() {
  const el = textareaRef.value;
  if (!el) return;
  const cs = getComputedStyle(el);
  const lineHeight = parseFloat(cs.lineHeight) || 24;
  const padTop = parseFloat(cs.paddingTop) || 0;
  const padBottom = parseFloat(cs.paddingBottom) || 0;
  const minH = padTop + padBottom + lineHeight * 1;
  const maxH = padTop + padBottom + lineHeight * MAX_TEXT_LINES;

  el.style.height = '0px';
  el.style.height = 'auto';
  const scrollH = el.scrollHeight;
  const next = Math.min(Math.max(scrollH, minH), maxH);
  el.style.height = `${next}px`;
  el.style.overflowY = scrollH > maxH ? 'auto' : 'hidden';
}

const handleSend = () => {
  if (props.disabled || !inputText.value.trim()) return;
  emit('send', {
    text: inputText.value.trim(),
    searchEnabled: searchEnabled.value,
    ragEnabled: false,
  });
  inputText.value = '';
  nextTick(() => adjustTextareaHeight());
};

watch(
  () => props.disabled,
  () => nextTick(() => adjustTextareaHeight()),
);

onMounted(() => {
  nextTick(() => adjustTextareaHeight());
});
</script>
