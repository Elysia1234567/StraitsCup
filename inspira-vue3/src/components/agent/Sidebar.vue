<template>
  <aside class="flex h-full w-64 flex-col border-r border-white/20 bg-transparent text-white">
    <div class="p-4">
      <div class="mb-4 flex items-center justify-between">
        <h1 class="text-xl font-bold text-white">同源</h1>
        <div class="flex gap-2">
          <button class="rounded p-1.5 transition hover:bg-white/10">
            <MagnifyingGlassIcon class="h-5 w-5 text-white/80" />
          </button>
          <button class="rounded p-1.5 transition hover:bg-white/10" type="button" @click="$emit('toggle-collapse')">
            <ChevronDoubleLeftIcon class="h-5 w-5 text-white/80" />
          </button>
        </div>
      </div>

      <button
        class="w-full rounded-lg border border-white/25 bg-transparent px-4 py-2.5 text-white transition hover:bg-white/10"
        type="button"
        @click="$emit('new-chat')"
      >
        <span class="flex items-center justify-center gap-2">
          <PlusIcon class="h-4 w-4" />
          <span>新建对话</span>
        </span>
      </button>

    </div>

    <div class="scrollbar-track-transparent flex-1 overflow-y-auto px-3">
      <div class="mb-6 space-y-1">
        <button
          class="flex w-full items-center gap-3 rounded-lg px-3 py-2 transition hover:bg-white/10"
          :class="{ 'bg-white/20': activeView === 'agents' }"
          type="button"
          @click="$emit('switch-view', 'agents')"
        >
          <Bars3BottomLeftIcon class="h-5 w-5 text-white/80" />
          <span>智能体</span>
        </button>
      </div>

      <div v-if="loadingRooms" class="mt-6 px-3 text-xs text-white/50">加载会话列表…</div>

      <div v-else-if="recentChats.length > 0" class="mt-6">
        <p class="mb-2 px-3 text-xs text-white/60">历史对话</p>
        <div class="space-y-1">
          <div
            v-for="chat in recentChats"
            :key="chat.id"
            class="group flex cursor-pointer items-center justify-between rounded-lg px-3 py-2 transition hover:bg-white/10"
            :class="{ 'bg-white/20': activeChatId === chat.id && activeView === 'chat' }"
            @click="$emit('select-chat', chat.id)"
          >
            <span class="min-w-0 flex-1 truncate text-sm">{{ chat.title }}</span>
            <button
              class="shrink-0 rounded p-1 text-white/50 opacity-0 transition hover:bg-rose-500/25 hover:text-rose-200 group-hover:opacity-100"
              type="button"
              title="删除会话"
              @click.stop="$emit('delete-chat', chat.id)"
            >
              <TrashIcon class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="border-t border-white/20 p-4">
      <div class="flex items-center gap-2">
        <div class="flex h-6 w-6 items-center justify-center rounded-full bg-white/20">
          <FaceSmileIcon class="h-4 w-4 text-white/70" />
        </div>
        <span class="text-sm text-white/80">Qwen1959</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import {
  MagnifyingGlassIcon,
  ChevronDoubleLeftIcon,
  PlusIcon,
  Bars3BottomLeftIcon,
  TrashIcon,
  FaceSmileIcon,
} from '@heroicons/vue/24/outline';

defineProps({
  recentChats: {
    type: Array,
    default: () => [],
  },
  activeChatId: {
    type: [Number, String],
    default: null,
  },
  activeView: {
    type: String,
    default: 'chat',
  },
  loadingRooms: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['new-chat', 'switch-view', 'select-chat', 'delete-chat', 'toggle-collapse']);
</script>
