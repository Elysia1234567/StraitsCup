<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import SiteHeader from '../components/SiteHeader.vue';
import ClientOnly from '../components/ClientOnly.vue';
import AgentBackground from '../components/ui/AgentBackground.vue';
import Sidebar from '../components/agent/Sidebar.vue';
import ChatInput from '../components/agent/ChatInput.vue';
import MessageBubble from '../components/agent/MessageBubble.vue';
import AgentCard from '../components/agent/AgentCard.vue';
import { ChevronDoubleRightIcon } from '@heroicons/vue/24/outline';
import { useMultiAgentChat } from '@/composables/useMultiAgentChat.js';
import * as chatApi from '@/api/chatApi.js';
import { toAgentViewModel } from '@/utils/agentAssets.js';

const LAST_ROOM_KEY = 'inspira_last_room_id';

const activeView = ref('chat');
const sidebarCollapsed = ref(false);

const {
  rooms,
  currentRoomId,
  roomChatAgents,
  messages,
  wsConnected,
  wsError,
  loadingRooms,
  loadingMessages,
  loadRooms,
  openRoom,
  createNewRoom,
  deleteChatRoom,
  sendUserMessage,
} = useMultiAgentChat();

const catalogAgents = ref([]);
const loadingAgents = ref(false);
const selectedAgentCodes = ref(new Set());
const bootError = ref(null);

const recentChats = computed(() =>
  rooms.value.map((r) => ({
    id: r.id,
    title: r.name || `会话 ${r.id}`,
  })),
);

const activeChatId = computed(() => currentRoomId.value);

const chatInputDisabled = computed(
  () => !currentRoomId.value || loadingMessages.value || !wsConnected.value,
);

const chatPlaceholder = computed(() => {
  if (!currentRoomId.value) return '请先新建对话或从历史中选择会话';
  if (loadingMessages.value) return '加载消息中…';
  if (!wsConnected.value) return '正在连接实时通道…';
  return '输入消息；可 @大家 或说「你们」让多智能体一起回答';
});

function messageKey(msg, i) {
  if (msg.messageId != null) return `m-${msg.messageId}`;
  if (msg.streamId) return `s-${msg.streamId}`;
  if (msg.pending) return `p-${i}-${msg.content?.slice(0, 12)}`;
  return `i-${i}`;
}

async function loadAgentCatalog() {
  loadingAgents.value = true;
  bootError.value = null;
  try {
    const list = await chatApi.fetchAgents();
    catalogAgents.value = (list || []).map((a) => ({
      id: a.id,
      agentCode: a.agentCode,
      name: a.name,
      ...toAgentViewModel(a),
      description: a.personality || a.knowledgeScope || a.roleType || '非遗文化主题智能体',
      official: true,
      new: false,
      usageCount: '—',
    }));
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
    catalogAgents.value = [];
  } finally {
    loadingAgents.value = false;
  }
}

/** 按勾选顺序排列，用于顶栏头像（选中一个展示一个） */
const selectedAgentsOrdered = computed(() => {
  const order = Array.from(selectedAgentCodes.value);
  const map = new Map(catalogAgents.value.map((a) => [a.agentCode, a]));
  return order.map((code) => map.get(code)).filter(Boolean);
});

function toggleAgentSelection(agent) {
  const code = agent.agentCode;
  if (!code) return;
  const next = new Set(selectedAgentCodes.value);
  if (next.has(code)) {
    next.delete(code);
  } else {
    next.add(code);
  }
  selectedAgentCodes.value = next;
}

async function bootstrap() {
  bootError.value = null;
  try {
    await loadRooms();
    const raw = localStorage.getItem(LAST_ROOM_KEY);
    const lastId = raw ? Number(raw) : NaN;
    const match = Number.isFinite(lastId) && rooms.value.some((r) => r.id === lastId);
    if (match) {
      await openRoom(lastId);
    } else if (rooms.value.length > 0) {
      await openRoom(rooms.value[0].id);
    }
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  }
}

onMounted(() => {
  bootstrap();
  loadAgentCatalog();
});

watch(currentRoomId, (id) => {
  if (id != null) {
    localStorage.setItem(LAST_ROOM_KEY, String(id));
  } else {
    localStorage.removeItem(LAST_ROOM_KEY);
  }
});

const handleSendMessage = (payload) => {
  activeView.value = 'chat';
  const text = typeof payload === 'string' ? payload : payload?.text;
  const searchEnabled = typeof payload === 'object' && payload ? !!payload.searchEnabled : false;
  const imageMode = typeof payload === 'object' && payload ? !!payload.imageMode : false;
  if (!text?.trim()) return;
  if (imageMode) {
    const primaryAgent = roomChatAgents.value?.[0];
    const agentCode = primaryAgent?.agentCode;
    if (!agentCode) {
      bootError.value = '当前房间没有可用的智能体';
      return;
    }
    chatApi.generateAgentImage({
      userId: 1,
      roomId: currentRoomId.value,
      prompt: text.trim(),
      agentCode,
    }).catch((e) => {
      bootError.value = e instanceof Error ? e.message : String(e);
    });
    return;
  }
  sendUserMessage(text, { searchEnabled });
};

const handleNewChat = async () => {
  activeView.value = 'chat';
  bootError.value = null;
  try {
    await createNewRoom({
      name: `新对话 ${new Date().toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}`,
    });
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  }
};

const handleSelectChat = async (id) => {
  activeView.value = 'chat';
  bootError.value = null;
  try {
    await openRoom(id);
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  }
};

const handleDeleteChat = async (id) => {
  if (!id) return;
  if (!window.confirm('确定删除该会话？删除后无法恢复。')) return;
  bootError.value = null;
  try {
    await deleteChatRoom(id);
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  }
};

const handleCreateGroupFromSelection = async () => {
  bootError.value = null;
  const codes = selectedAgentCodes.value.size > 0 ? Array.from(selectedAgentCodes.value) : undefined;
  try {
    await createNewRoom({
      name: `群聊 ${new Date().toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}`,
      agentCodes: codes,
    });
    selectedAgentCodes.value = new Set();
    activeView.value = 'chat';
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  }
};
</script>

<template>
  <main class="relative min-h-screen overflow-hidden bg-slate-950 text-white">
    <ClientOnly>
      <AgentBackground
        class="absolute inset-0 z-0"
        color="#a78bfa"
        :quantity="120"
        :staticity="45"
        :ease="55"
      />
    </ClientOnly>

    <SiteHeader />

    <section class="relative z-10 flex h-screen pt-16 text-white">
      <Sidebar
        v-if="!sidebarCollapsed"
        :recent-chats="recentChats"
        :active-chat-id="activeChatId"
        :active-view="activeView"
        :loading-rooms="loadingRooms"
        @new-chat="handleNewChat"
        @switch-view="activeView = $event"
        @select-chat="handleSelectChat"
        @delete-chat="handleDeleteChat"
        @toggle-collapse="sidebarCollapsed = true"
      />
      <button
        v-else
        class="absolute left-3 top-24 z-20 rounded p-1.5 transition hover:bg-white/10"
        type="button"
        @click="sidebarCollapsed = false"
      >
        <ChevronDoubleRightIcon class="h-5 w-5 text-white/80" />
      </button>

      <section class="flex flex-1 flex-col bg-transparent text-white">
        <div
          v-if="bootError || wsError"
          class="border-b border-amber-500/30 bg-amber-500/10 px-4 py-2 text-center text-sm text-amber-100"
        >
          {{ bootError || wsError }}
        </div>

        <!-- 对话顶栏：有会话时展示房间内智能体；在智能体库视图展示当前勾选（选一显一） -->
        <div
          v-if="activeView === 'agents' || currentRoomId"
          class="flex shrink-0 items-center justify-between gap-3 border-b border-white/10 px-6 py-2.5"
        >
          <div class="flex min-w-0 flex-1 flex-wrap items-center justify-start gap-2">
            <template v-if="activeView === 'agents'">
              <img
                v-for="agent in selectedAgentsOrdered"
                :key="agent.agentCode"
                :src="agent.avatar"
                :alt="agent.name"
                :title="agent.name"
                class="h-9 w-9 shrink-0 rounded-full object-cover ring-1 ring-white/25"
              />
            </template>
            <template v-else>
              <img
                v-for="agent in roomChatAgents"
                :key="agent.id"
                :src="agent.avatar"
                :alt="agent.name"
                :title="agent.name"
                class="h-9 w-9 shrink-0 rounded-full object-cover ring-1 ring-white/25"
              />
              <span v-if="!loadingMessages && roomChatAgents.length === 0" class="text-xs text-white/45">
                当前房间暂无智能体成员
              </span>
            </template>
          </div>
          <button
            v-if="activeView === 'agents'"
            class="shrink-0 rounded-xl bg-violet-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-violet-500 disabled:opacity-40"
            type="button"
            :disabled="loadingAgents"
            @click="handleCreateGroupFromSelection"
          >
            创建群聊房间
          </button>
        </div>

        <div v-if="activeView === 'chat'" class="flex h-full min-h-0 flex-col">
          <div class="scrollbar-track-transparent min-h-0 flex-1 overflow-y-auto p-6">
            <div v-if="loadingMessages" class="flex h-full items-center justify-center text-white/50">加载历史中…</div>

            <div v-else-if="!currentRoomId" class="flex h-full flex-col items-center justify-center px-4 text-center">
              <div class="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-qwen">
                <svg class="h-8 w-8 text-white" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
                </svg>
              </div>
              <h2 class="mb-3 text-2xl font-bold text-white">多智能体对话</h2>
              <p class="mb-8 max-w-md text-sm text-white/65">
                连接 StraitsCup 后端：REST 拉取会话与历史，WebSocket 接收用户广播与多 Agent 流式分片（AGENT_CHUNK）。
              </p>
              <button
                class="rounded-xl bg-violet-600 px-6 py-2.5 text-sm font-medium text-white transition hover:bg-violet-500"
                type="button"
                @click="handleNewChat"
              >
                新建对话
              </button>
            </div>

            <div v-else-if="messages.length === 0" class="flex h-full flex-col items-center justify-center">
              <h2 class="mb-8 text-3xl font-bold text-white">你好</h2>
              <p class="max-w-lg text-center text-sm text-white/60">
                当前房间已连接。可直接提问；在句子里带上「你们」「大家」等词可触发房间内全部智能体回复（与后端编排一致）。
              </p>
            </div>

            <div v-else class="mx-auto max-w-3xl">
              <MessageBubble v-for="(message, index) in messages" :key="messageKey(message, index)" :message="message" />
            </div>
          </div>

          <div class="shrink-0 p-6">
            <ChatInput
              :disabled="chatInputDisabled"
              :placeholder="chatPlaceholder"
              @send="handleSendMessage"
            />
          </div>
        </div>

        <div v-else class="flex h-full min-h-0 flex-col">
          <div class="scrollbar-track-transparent min-h-0 flex-1 overflow-y-auto p-8">
            <div class="mx-auto max-w-5xl">
              <div class="mb-8 flex gap-6">
                <button
                  class="border-b-2 pb-2 font-medium transition"
                  :class="activeView === 'agents' ? 'border-white text-white' : 'border-transparent text-white/70 hover:text-white'"
                  type="button"
                  @click="activeView = 'agents'"
                >
                  智能体库
                </button>
              </div>

              <div v-if="loadingAgents" class="py-20 text-center text-white/50">加载智能体列表…</div>
              <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <AgentCard
                  v-for="agent in catalogAgents"
                  :key="agent.id"
                  :agent="agent"
                  selectable
                  :selected="selectedAgentCodes.has(agent.agentCode)"
                  @toggle="toggleAgentSelection"
                />
              </div>
            </div>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>
