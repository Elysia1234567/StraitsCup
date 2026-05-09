<template>
  <div v-if="message.role === 'system'" class="mb-4 flex justify-center px-4">
    <p class="max-w-xl text-center text-xs text-white/50">
      {{ message.content }}
      <span v-if="message.onlineCount != null" class="text-white/40">（在线 {{ message.onlineCount }}）</span>
    </p>
  </div>

  <div v-else class="mb-6 flex gap-3" :class="{ 'flex-row-reverse': message.role === 'user' }">
    <img
      v-if="message.role === 'assistant' && message.senderAvatar"
      :src="message.senderAvatar"
      :alt="message.senderName || ''"
      class="mt-1 h-9 w-9 shrink-0 rounded-full object-cover ring-1 ring-white/20"
    />
    <div
      v-else-if="message.role === 'assistant'"
      class="mt-1 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/15 text-xs text-white/80 ring-1 ring-white/20"
    >
      AI
    </div>

    <div
      class="max-w-[min(100%,42rem)] rounded-2xl px-4 py-3"
      :class="message.role === 'user' ? 'bg-white/20 text-white' : 'border border-white/20 bg-white/5 text-white'"
    >
      <p v-if="message.role === 'assistant' && message.senderName" class="mb-1 text-xs font-medium text-violet-200/90">
        {{ message.senderName }}
      </p>

      <div v-if="message.imageUrl" class="mb-2 overflow-hidden rounded-lg border border-white/10">
        <img :src="message.imageUrl" alt="" class="max-h-64 w-full object-cover" />
      </div>

      <p class="whitespace-pre-wrap">{{ message.content }}</p>
      <span v-if="message.role === 'assistant' && message.streaming" class="ml-0.5 inline-block h-4 w-1 animate-pulse bg-violet-300/80 align-middle" />

      <div
        v-if="message.role === 'assistant' && !message.streaming"
        class="mt-3 flex flex-wrap items-center gap-2 border-t border-white/15 pt-3"
      >
        <button
          class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs transition"
          :class="copyDone ? 'bg-emerald-500/15 text-emerald-100' : 'text-white/75 hover:bg-white/10'"
          type="button"
          title="复制"
          @click="handleCopy"
        >
          <CheckIcon v-if="copyDone" class="h-4 w-4" />
          <DocumentDuplicateIcon v-else class="h-4 w-4" />
          <span>{{ copyDone ? '已复制' : '复制' }}</span>
        </button>

        <button
          class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs transition"
          :class="feedbackStatus === 1 ? 'bg-emerald-500/20 text-emerald-100 ring-1 ring-emerald-300/30' : 'text-white/75 hover:bg-white/10'"
          type="button"
          :disabled="!canFeedback"
          title="赞"
          @click="handleFeedback(1)"
        >
          <HandThumbUpSolidIcon v-if="feedbackStatus === 1" class="h-4 w-4" />
          <HandThumbUpIcon v-else class="h-4 w-4" />
          <span>{{ feedbackStatus === 1 ? '已赞' : '赞' }}</span>
        </button>

        <button
          class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs transition"
          :class="feedbackStatus === -1 ? 'bg-rose-500/20 text-rose-100 ring-1 ring-rose-300/30' : 'text-white/75 hover:bg-white/10'"
          type="button"
          :disabled="!canFeedback"
          title="踩"
          @click="handleFeedback(-1)"
        >
          <HandThumbDownSolidIcon v-if="feedbackStatus === -1" class="h-4 w-4" />
          <HandThumbDownIcon v-else class="h-4 w-4" />
          <span>{{ feedbackStatus === -1 ? '已踩' : '踩' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue';
import {
  DocumentDuplicateIcon,
  HandThumbUpIcon,
  HandThumbDownIcon,
  CheckIcon,
} from '@heroicons/vue/24/outline';
import {
  HandThumbUpIcon as HandThumbUpSolidIcon,
  HandThumbDownIcon as HandThumbDownSolidIcon,
} from '@heroicons/vue/24/solid';

const props = defineProps({
  message: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['feedback-change']);

const copyDone = ref(false);
let copyTimer = null;

const feedbackStatus = computed(() => props.message.feedbackStatus ?? 0);
const canFeedback = computed(() => Boolean(props.message.messageId ?? props.message.id));

function getMessageId() {
  return props.message.messageId ?? props.message.id ?? null;
}

async function handleCopy() {
  const text = props.message.content || '';
  if (!text) return;

  try {
    await copyTextToClipboard(text);
    copyDone.value = true;
    if (copyTimer) clearTimeout(copyTimer);
    copyTimer = window.setTimeout(() => {
      copyDone.value = false;
    }, 1500);
  } catch {
    copyDone.value = false;
  }
}

function handleFeedback(nextStatus) {
  const messageId = getMessageId();
  if (!messageId) return;
  const currentStatus = feedbackStatus.value;
  const finalStatus = currentStatus === nextStatus ? 0 : nextStatus;
  emit('feedback-change', {
    messageId,
    feedbackStatus: finalStatus,
    message: props.message,
  });
}

async function copyTextToClipboard(text) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', 'true');
  textarea.style.position = 'fixed';
  textarea.style.left = '-9999px';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
}

onBeforeUnmount(() => {
  if (copyTimer) clearTimeout(copyTimer);
});
</script>
