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

      <details
        v-if="message.role === 'assistant' && !message.streaming && hasEvidence"
        class="mt-3 rounded-lg border border-white/15 bg-black/15 text-xs text-white/75"
      >
        <summary class="flex cursor-pointer list-none items-center justify-between gap-3 px-3 py-2 text-white/85">
          <span class="inline-flex min-w-0 items-center gap-2">
            <ShieldCheckIcon class="h-4 w-4 shrink-0 text-cyan-200" />
            <span class="truncate">依据与可信度</span>
          </span>
          <span
            class="shrink-0 rounded px-1.5 py-0.5"
            :class="confidenceBadgeClass"
          >
            {{ confidenceLabel }}
          </span>
        </summary>

        <div class="space-y-3 border-t border-white/10 px-3 py-3">
          <p v-if="confidenceReason" class="leading-relaxed text-white/60">
            {{ confidenceReason }}
          </p>

          <section v-if="ragSources.length" class="space-y-2">
            <div class="flex items-center gap-1.5 font-medium text-cyan-100/90">
              <CircleStackIcon class="h-4 w-4" />
              <span>RAG 来源</span>
            </div>
            <div class="space-y-2">
              <article
                v-for="source in ragSources"
                :key="source.id || source.title"
                class="rounded-md border border-cyan-200/10 bg-cyan-300/5 px-2.5 py-2"
              >
                <div class="flex flex-wrap items-center gap-2">
                  <span class="font-medium text-white/90">{{ source.title || '未命名资料' }}</span>
                  <span v-if="source.score != null" class="rounded bg-white/10 px-1.5 py-0.5 text-[11px] text-white/55">
                    {{ formatScore(source.score) }}
                  </span>
                  <span v-if="source.region" class="text-[11px] text-white/45">{{ source.region }}</span>
                  <span v-if="source.category" class="text-[11px] text-white/45">{{ source.category }}</span>
                </div>
                <p v-if="source.excerpt" class="mt-1.5 line-clamp-3 leading-relaxed text-white/60">
                  {{ source.excerpt }}
                </p>
              </article>
            </div>
          </section>

          <section v-if="webSources.length || webSummary" class="space-y-2">
            <div class="flex items-center gap-1.5 font-medium text-violet-100/90">
              <LinkIcon class="h-4 w-4" />
              <span>联网来源</span>
            </div>
            <p v-if="webSummary" class="rounded-md bg-violet-300/5 px-2.5 py-2 leading-relaxed text-white/60">
              {{ webSummary }}
            </p>
            <div v-if="webSources.length" class="space-y-2">
              <article
                v-for="source in webSources"
                :key="source.url || source.title"
                class="rounded-md border border-violet-200/10 bg-violet-300/5 px-2.5 py-2"
              >
                <a
                  v-if="source.url"
                  :href="source.url"
                  target="_blank"
                  rel="noreferrer"
                  class="break-words font-medium text-violet-100 underline decoration-violet-200/30 underline-offset-2 hover:text-white"
                >
                  {{ source.title || source.url }}
                </a>
                <p v-else class="font-medium text-white/90">{{ source.title || '网页来源' }}</p>
                <p v-if="source.url" class="mt-1 truncate text-[11px] text-white/40">{{ getHost(source.url) }}</p>
                <p v-if="source.excerpt" class="mt-1.5 line-clamp-3 leading-relaxed text-white/60">
                  {{ source.excerpt }}
                </p>
              </article>
            </div>
          </section>
        </div>
      </details>

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
  ShieldCheckIcon,
  CircleStackIcon,
  LinkIcon,
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
const evidence = computed(() => props.message.metadata || {});
const confidence = computed(() => evidence.value.confidence || {});
const confidenceLabel = computed(() => `可信度：${confidence.value.level || '未评估'}`);
const confidenceReason = computed(() => confidence.value.reason || '');
const ragSources = computed(() => evidence.value.rag?.sources || []);
const webSources = computed(() => evidence.value.webSearch?.sources || []);
const webSummary = computed(() => evidence.value.webSearch?.summary || '');
const hasEvidence = computed(() =>
  Boolean(confidence.value.level || ragSources.value.length || webSources.value.length || webSummary.value),
);
const confidenceBadgeClass = computed(() => {
  if (confidence.value.level === '高') return 'bg-emerald-400/15 text-emerald-100 ring-1 ring-emerald-300/25';
  if (confidence.value.level === '中') return 'bg-amber-400/15 text-amber-100 ring-1 ring-amber-300/25';
  if (confidence.value.level === '低') return 'bg-rose-400/15 text-rose-100 ring-1 ring-rose-300/25';
  return 'bg-white/10 text-white/60';
});

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

function formatScore(score) {
  const value = Number(score);
  if (!Number.isFinite(value)) return '';
  return `相似度 ${value.toFixed(2)}`;
}

function getHost(url) {
  try {
    return new URL(url).host;
  } catch {
    return url;
  }
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
