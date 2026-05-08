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
        <div class="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto">
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
          <button
            class="flex shrink-0 items-center gap-0.5 rounded-md px-1.5 py-0.5 transition hover:bg-white/10"
            :class="{ 'bg-white/15 ring-1 ring-white/20': imageMode }"
            type="button"
            :disabled="disabled"
            @click="imageMode = !imageMode"
          >
            <PhotoIcon class="h-3.5 w-3.5 text-white/80" />
            <span class="text-xs text-white/80">AI生图</span>
          </button>
        </div>

        <div class="flex shrink-0 items-center gap-1 pl-1">
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
import { MagnifyingGlassIcon, PhotoIcon, ArrowUpIcon } from '@heroicons/vue/24/outline';

const inputText = ref('');
const searchEnabled = ref(false);
const imageMode = ref(false);
const textareaRef = ref(null);

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
  const minH = padTop + padBottom + lineHeight;
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
    imageMode: imageMode.value,
  });
  inputText.value = '';
  imageMode.value = false;
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
