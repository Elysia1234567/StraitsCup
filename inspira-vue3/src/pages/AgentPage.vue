<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import ClientOnly from '../components/ClientOnly.vue';
import AgentBackground from '../components/ui/AgentBackground.vue';
import Sidebar from '../components/agent/Sidebar.vue';
import ChatInput from '../components/agent/ChatInput.vue';
import MessageBubble from '../components/agent/MessageBubble.vue';
import AgentCard from '../components/agent/AgentCard.vue';
import { ChevronDoubleRightIcon, PhotoIcon, SparklesIcon } from '@heroicons/vue/24/outline';
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
  addRoomAgentToCurrentRoom,
  removeRoomAgentFromCurrentRoom,
  sendUserMessage,
} = useMultiAgentChat();

const catalogAgents = ref([]);
const loadingAgents = ref(false);
const selectedAgentCodes = ref(new Set());
const bootError = ref(null);
const imagePrompt = ref('');
const generatingImage = ref(false);
const selectedImageAgentCode = ref('');
const roomCreationMode = ref(false);
const roomManageMode = ref(false);

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

const imageAgent = computed(() => {
  if (!selectedImageAgentCode.value) return null;
  return roomChatAgents.value.find((agent) => agent.agentCode === selectedImageAgentCode.value) || null;
});

const currentRoomAgentCodes = computed(() => new Set(roomChatAgents.value.map((agent) => agent.agentCode).filter(Boolean)));

const availableAgentsToAdd = computed(() =>
  catalogAgents.value.filter((agent) => !currentRoomAgentCodes.value.has(agent.agentCode)),
);

const selectedCount = computed(() => selectedAgentCodes.value.size);
const remainingRoomAgentSlots = computed(() => Math.max(0, 6 - roomChatAgents.value.length));
const agentActionDisabled = computed(() => {
  if (loadingAgents.value) return true;
  if (roomManageMode.value) {
    return selectedCount.value < 1 || selectedCount.value > remainingRoomAgentSlots.value;
  }
  return selectedCount.value < 1 || selectedCount.value > 6;
});

const imagePlaceholder = computed(() => {
  if (!currentRoomId.value) return '请先新建或选择一个对话房间';
  if (!imageAgent.value) return '当前房间没有可用于生图的智能体';
  return `描述你想让「${imageAgent.value.name}」生成的画面`;
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
  } else if (roomCreationMode.value && next.size >= 6) {
    bootError.value = '创建聊天室最多选择 6 个 Agent';
    return;
  } else if (roomManageMode.value && next.size >= Math.max(0, 6 - roomChatAgents.value.length)) {
    bootError.value = '当前聊天室最多还能添加 ' + Math.max(0, 6 - roomChatAgents.value.length) + ' 个 Agent';
    return;
  } else {
    next.add(code);
  }
  selectedAgentCodes.value = next;
  bootError.value = null;
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
  if (!text?.trim()) return;
  sendUserMessage(text, { searchEnabled });
};

const handleMessageFeedback = async ({ messageId, feedbackStatus, message }) => {
  if (!currentRoomId.value || !messageId || !message) return;
  const previousStatus = message.feedbackStatus ?? 0;
  message.feedbackStatus = feedbackStatus;
  try {
    await chatApi.updateMessageFeedback(currentRoomId.value, messageId, feedbackStatus);
  } catch (e) {
    message.feedbackStatus = previousStatus;
    bootError.value = e instanceof Error ? e.message : String(e);
  }
};

const handleGenerateImage = async () => {
  const prompt = imagePrompt.value.trim();
  if (!prompt || generatingImage.value) return;
  if (!currentRoomId.value) {
    bootError.value = '请先新建或选择一个对话房间';
    return;
  }
  const agentCode = imageAgent.value?.agentCode;
  if (!agentCode) {
    bootError.value = '当前房间没有可用的智能体';
    return;
  }

  bootError.value = null;
  generatingImage.value = true;
  try {
    await chatApi.generateAgentImage({
      userId: 1,
      roomId: currentRoomId.value,
      prompt,
      agentCode,
    });
    imagePrompt.value = '';
    activeView.value = 'chat';
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  } finally {
    generatingImage.value = false;
  }
};

const handleNewChat = async () => {
  activeView.value = 'agents';
  bootError.value = null;
  roomCreationMode.value = true;
  roomManageMode.value = false;
  selectedAgentCodes.value = new Set();
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
  const codes = Array.from(selectedAgentCodes.value);
  if (codes.length < 1 || codes.length > 6) {
    bootError.value = '创建聊天室需要选择 1 到 6 个 Agent';
    return;
  }
  try {
    await createNewRoom({
      name: `新对话 ${new Date().toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}`,
      agentCodes: codes,
    });
    selectedAgentCodes.value = new Set();
    roomCreationMode.value = false;
    activeView.value = 'chat';
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  }
};

const handleAddSelectedAgentsToRoom = async () => {
  bootError.value = null;
  if (!currentRoomId.value) {
    bootError.value = '请先选择一个聊天室';
    return;
  }
  const codes = Array.from(selectedAgentCodes.value);
  if (codes.length === 0) {
    bootError.value = '请选择要添加的 Agent';
    return;
  }
  if (roomChatAgents.value.length + codes.length > 6) {
    bootError.value = '聊天室最多选择 6 个 Agent';
    return;
  }
  try {
    for (const code of codes) {
      await addRoomAgentToCurrentRoom(code);
    }
    selectedAgentCodes.value = new Set();
    roomManageMode.value = false;
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  }
};

const handleRemoveRoomAgent = async (agent) => {
  if (roomChatAgents.value.length <= 1) {
    bootError.value = '聊天室至少保留一个 Agent';
    return;
  }
  bootError.value = null;
  try {
    await removeRoomAgentFromCurrentRoom(agent.id);
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : String(e);
  }
};

const openRoomAgentManage = () => {
  if (!currentRoomId.value) {
    bootError.value = '请先选择一个聊天室';
    return;
  }
  activeView.value = 'agents';
  roomCreationMode.value = false;
  roomManageMode.value = true;
  selectedAgentCodes.value = new Set();
};

watch(roomChatAgents, (agents) => {
  if (!agents.some((agent) => agent.agentCode === selectedImageAgentCode.value)) {
    selectedImageAgentCode.value = agents[0]?.agentCode || '';
  }
}, { immediate: true });
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

        <!-- 顶栏：对话/生图展示房间内智能体；智能体库展示当前勾选（选一显一） -->
        <div
          v-if="activeView === 'agents' || activeView === 'image' || currentRoomId"
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
              <div
                v-for="agent in roomChatAgents"
                :key="agent.id"
                class="group relative"
              >
                <img
                  :src="agent.avatar"
                  :alt="agent.name"
                  :title="agent.name"
                  class="h-9 w-9 shrink-0 rounded-full object-cover ring-1 ring-white/25"
                />
                <button
                  class="absolute -right-1 -top-1 hidden h-4 w-4 items-center justify-center rounded-full bg-rose-500 text-[10px] leading-none text-white shadow group-hover:flex disabled:opacity-40"
                  type="button"
                  title="移除智能体"
                  :disabled="roomChatAgents.length <= 1"
                  @click="handleRemoveRoomAgent(agent)"
                >
                  ×
                </button>
              </div>
              <span v-if="!loadingMessages && roomChatAgents.length === 0" class="text-xs text-white/45">
                当前房间暂无智能体成员
              </span>
              <button
                v-if="currentRoomId && roomChatAgents.length < 6"
                class="flex h-9 w-9 items-center justify-center rounded-full border border-dashed border-white/30 text-lg text-white/70 transition hover:bg-white/10"
                type="button"
                title="添加智能体"
                @click="openRoomAgentManage"
              >
                +
              </button>
            </template>
          </div>
          <button
            v-if="activeView === 'agents'"
            class="shrink-0 rounded-xl bg-violet-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-violet-500 disabled:opacity-40"
            type="button"
            :disabled="agentActionDisabled"
            @click="roomManageMode ? handleAddSelectedAgentsToRoom() : handleCreateGroupFromSelection()"
          >
            {{ roomManageMode ? `添加 ${selectedCount} 个` : `创建对话（${selectedCount}/6）` }}
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
              <MessageBubble
                v-for="(message, index) in messages"
                :key="messageKey(message, index)"
                :message="message"
                @feedback-change="handleMessageFeedback"
              />
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

        <div v-else-if="activeView === 'image'" class="flex h-full min-h-0 flex-col">
          <div class="scrollbar-track-transparent min-h-0 flex-1 overflow-y-auto p-8">
            <div class="mx-auto flex max-w-3xl flex-col gap-6">
              <div class="flex items-center gap-3">
                <div class="flex h-11 w-11 items-center justify-center rounded-lg border border-white/20 bg-white/10">
                  <PhotoIcon class="h-6 w-6 text-violet-100" />
                </div>
                <div>
                  <h2 class="text-xl font-semibold text-white">AI 生图</h2>
                  <p class="text-sm text-white/55">独立于普通对话，生成结果会作为图片消息进入当前房间。</p>
                </div>
              </div>

              <div class="border-y border-white/10 py-4">
                <div v-if="imageAgent" class="flex items-center gap-3">
                  <img :src="imageAgent.avatar" :alt="imageAgent.name" class="h-12 w-12 rounded-full object-cover ring-1 ring-white/25" />
                  <div class="min-w-0">
                    <p class="text-sm text-white/55">当前生图智能体</p>
                    <p class="truncate font-medium text-white">{{ imageAgent.name }}</p>
                  </div>
                </div>
                <p v-else class="text-sm text-white/55">当前房间暂无智能体，请先创建包含智能体的房间。</p>
              </div>

              <div class="rounded-lg border border-white/20 bg-white/5 p-4">
                <label class="mb-2 block text-sm text-white/70" for="image-agent-select">选择生图 Agent</label>
                <select
                  id="image-agent-select"
                  v-model="selectedImageAgentCode"
                  class="mb-3 w-full rounded-md border border-white/15 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none focus:border-violet-300/60"
                  :disabled="generatingImage || roomChatAgents.length === 0"
                >
                  <option value="" disabled>请选择 Agent</option>
                  <option
                    v-for="agent in roomChatAgents"
                    :key="agent.agentCode"
                    :value="agent.agentCode"
                  >
                    {{ agent.name }}
                  </option>
                </select>
                <textarea
                  v-model="imagePrompt"
                  class="min-h-36 w-full resize-y rounded-md border border-white/15 bg-transparent px-3 py-2 text-sm leading-6 text-white outline-none placeholder:text-white/45 focus:border-violet-300/60"
                  :placeholder="imagePlaceholder"
                  :disabled="generatingImage || !currentRoomId || !imageAgent"
                />
                <div class="mt-3 flex justify-end">
                  <button
                    class="inline-flex items-center gap-2 rounded-md bg-violet-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-violet-500 disabled:opacity-40"
                    type="button"
                    :disabled="generatingImage || !imagePrompt.trim() || !currentRoomId || !imageAgent"
                    @click="handleGenerateImage"
                  >
                    <SparklesIcon class="h-4 w-4" />
                    <span>{{ generatingImage ? '生成中…' : '生成图片' }}</span>
                  </button>
                </div>
              </div>
            </div>
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
                <span v-if="roomCreationMode" class="pb-2 text-sm text-white/60">请选择 1 到 6 个 Agent 创建聊天室</span>
                <span v-else-if="roomManageMode" class="pb-2 text-sm text-white/60">选择要添加到当前聊天室的 Agent</span>
              </div>

              <div
                v-if="currentRoomId && !roomCreationMode"
                class="mb-6 border-y border-white/10 py-4"
              >
                <div class="mb-3 flex items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-medium text-white">当前聊天室 Agent</p>
                    <p class="text-xs text-white/50">{{ roomChatAgents.length }}/6，可添加或移除，至少保留 1 个</p>
                  </div>
                  <button
                    class="rounded-md border border-white/20 px-3 py-1.5 text-sm text-white/80 transition hover:bg-white/10 disabled:opacity-40"
                    type="button"
                    :disabled="roomChatAgents.length >= 6"
                    @click="openRoomAgentManage"
                  >
                    添加 Agent
                  </button>
                </div>
                <div class="flex flex-wrap gap-2">
                  <div
                    v-for="agent in roomChatAgents"
                    :key="agent.id"
                    class="flex items-center gap-2 rounded-md border border-white/15 bg-white/5 px-2 py-1.5"
                  >
                    <img :src="agent.avatar" :alt="agent.name" class="h-7 w-7 rounded-full object-cover" />
                    <span class="text-sm text-white">{{ agent.name }}</span>
                    <button
                      class="rounded px-1 text-sm text-white/45 transition hover:bg-rose-500/20 hover:text-rose-100 disabled:opacity-30"
                      type="button"
                      :disabled="roomChatAgents.length <= 1"
                      @click="handleRemoveRoomAgent(agent)"
                    >
                      删除
                    </button>
                  </div>
                </div>
              </div>

              <div v-if="loadingAgents" class="py-20 text-center text-white/50">加载智能体列表…</div>
              <div v-else-if="roomManageMode && availableAgentsToAdd.length === 0" class="py-20 text-center text-white/50">
                当前聊天室已没有可添加的 Agent
              </div>
              <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <AgentCard
                  v-for="agent in roomManageMode ? availableAgentsToAdd : catalogAgents"
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
