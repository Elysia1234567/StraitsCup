<template>
  <div
    v-if="message.role === 'system'"
    class="mb-4 flex justify-center px-4"
  >
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
      :class="
        message.role === 'user'
          ? 'bg-white/20 text-white'
          : 'border border-white/20 bg-white/5 text-white'
      "
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
        <button class="rounded p-1 transition hover:bg-white/10" type="button" title="复制">
          <DocumentDuplicateIcon class="h-4 w-4 text-white/70" />
        </button>
        <button class="rounded p-1 transition hover:bg-white/10" type="button">
          <HandThumbUpIcon class="h-4 w-4 text-white/70" />
        </button>
        <button class="rounded p-1 transition hover:bg-white/10" type="button">
          <HandThumbDownIcon class="h-4 w-4 text-white/70" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { DocumentDuplicateIcon, HandThumbUpIcon, HandThumbDownIcon } from '@heroicons/vue/24/outline';

defineProps({
  message: {
    type: Object,
    required: true,
  },
});
</script>
