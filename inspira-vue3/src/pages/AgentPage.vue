<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  AtSymbolIcon,
  ArrowsPointingOutIcon,
  BookOpenIcon,
  ChatBubbleLeftRightIcon,
  CheckCircleIcon,
  CircleStackIcon,
  ClockIcon,
  CpuChipIcon,
  CubeTransparentIcon,
  DocumentTextIcon,
  GlobeAltIcon,
  LinkIcon,
  ListBulletIcon,
  MagnifyingGlassIcon,
  PaperAirplaneIcon,
  PhotoIcon,
  PlusIcon,
  ShieldCheckIcon,
  SparklesIcon,
  TrashIcon,
  UserPlusIcon,
  UsersIcon,
  XMarkIcon,
} from '@heroicons/vue/24/outline';
import { useRoute } from 'vue-router';
import { useMultiAgentChat } from '@/composables/useMultiAgentChat.js';
import { toAgentViewModel } from '@/utils/agentAssets.js';
import * as chatApi from '@/api/chatApi.js';

const LAST_ROOM_KEY = 'inspira_last_room_id';
const route = useRoute();

const insightTab = ref('evidence');
const agentPickerOpen = ref(false);
const roomManageMode = ref(false);
const bootError = ref('');
const roomInsight = ref(null);
const loadingInsight = ref(false);
const selectedAgentCodes = ref(new Set());
const inputText = ref('');
const mentionAll = ref(true);
const searchEnabled = ref(true);
const attachmentInput = ref(null);
const attachment = ref(null);
const attachmentUploading = ref(false);
const chatScrollRef = ref(null);
let insightRefreshTimer = null;

const {
  rooms,
  currentRoomId,
  roomChatAgents,
  messages,
  chatRevision,
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

const DEMO_AGENT_DEFINITIONS = [
  {
    agentCode: 'fz_cork_scene',
    name: '软木画传承人',
    tag: '工艺专家',
    description: '擅长软木画的工艺流程、材料特性与技法解析',
  },
  {
    agentCode: 'fz_lacquerware',
    name: '漆器工艺师',
    tag: '材料专家',
    description: '精通中国传统漆器工艺、材料与制作技法',
  },
  {
    agentCode: 'xm_wangchuan',
    name: '海洋文化研究员',
    tag: '历史学者',
    description: '研究海洋丝绸之路历史、文物与文化交流',
  },
  {
    agentCode: 'ly_hakka_embroidery',
    name: '传统刺绣匠人',
    tag: '民俗专家',
    description: '熟悉纹样寓意、装饰结构与民间应用',
  },
  {
    agentCode: 'fz_shoushan_stone',
    name: '非遗数字馆员',
    tag: '知识检索',
    description: '擅长非遗知识检索、图谱梳理与证据归纳',
  },
  {
    agentCode: 'fz_lacquerware',
    name: '石雕匠人',
    tag: '雕刻专家',
    description: '专注刀法层次、材质塑形与代表作品解读',
  },
];

const DEMO_MESSAGES = [
  {
    role: 'user',
    senderName: '用户',
    content: '请介绍软木画这项非遗的历史来源、制作工艺、代表作品和当代传承现状。',
    time: '16:58',
  },
  {
    role: 'assistant',
    senderName: '软木画传承人',
    senderId: 'fz_cork_scene',
    content:
      '软木画起源于福建福州，已有三百多年历史。以天然软木为原料，雕刻、拼贴成画，题材多取自山水、花鸟、人物。制作工艺包括：选材 → 设计 → 雕刻 → 拼贴 → 打磨 → 上色。',
    streaming: true,
    time: '16:58',
    metadata: {
      confidence: {
        level: '高',
        score: 0.98,
        reason: 'RAG 命中多条资料且最高相似度较高',
      },
      rag: {
        enabled: true,
        sources: [
          {
            id: 'demo-rag-1',
            title: '国家级非遗名录 - 软木画',
            score: 0.98,
            excerpt: '福州软木画以软木为材，雕刻与拼贴结合，具有立体层次与装饰性。',
            region: '福州',
            category: '官方',
          },
          {
            id: 'demo-rag-2',
            title: '福州工艺美术志',
            score: 0.94,
            excerpt: '工艺流程包括选材、设计、雕刻、拼贴、打磨和上色。',
            region: '福州',
            category: '图文',
          },
          {
            id: 'demo-rag-3',
            title: '松鹤图作品研究',
            score: 0.9,
            excerpt: '代表作品常以山水、花鸟、人物为主题，形成装饰性强的画面语言。',
            region: '福州',
            category: '论文',
          },
        ],
      },
      webSearch: {
        enabled: true,
        summary: '可见资料一致指向福州起源、软木材质、拼贴成画与工艺美术价值。',
        sources: [
          {
            title: '福州软木画工艺流程图解',
            url: 'https://example.com/softwood-art',
            excerpt: '工艺精细，兼具装饰性与实用审美。',
          },
          {
            title: '软木画代表作品《松鹤图》',
            url: 'https://example.com/songhe',
            excerpt: '层次分明，细节丰富，体现民间审美。',
          },
        ],
      },
    },
  },
  {
    role: 'assistant',
    senderName: '漆器工艺师',
    senderId: 'fz_lacquerware',
    content: '漆器常用天然漆与木质胎器，纹理细腻，便于髹涂、打磨与装饰。传统上会配合大漆进行表面保护与色彩增强，形成温润而深邃的光泽。',
    streaming: true,
    time: '16:58',
  },
  {
    role: 'assistant',
    senderName: '海洋文化研究员',
    senderId: 'xm_wangchuan',
    content: '该类工艺在海上丝绸之路时期常作为外销工艺品远销东南亚、西亚和欧洲，兼具地方审美与交流价值。',
    streaming: true,
    time: '16:58',
  },
  {
    role: 'assistant',
    senderName: '传统刺绣匠人',
    senderId: 'ly_hakka_embroidery',
    content: '软木画常见题材包括松鹤延年、渔樵耕读、山水亭台等，寓意吉祥，也常被用于礼品与家居装饰。',
    streaming: true,
    time: '16:58',
  },
  {
    role: 'assistant',
    senderName: '非遗数字馆员',
    senderId: 'fz_shoushan_stone',
    content: '已检索到相关资料 32 条、图片 18 张、视频 6 段，来源覆盖国家非遗名录、地方志、博物馆馆藏与学术论文，证据链将在右侧展开。',
    streaming: true,
    time: '16:58',
  },
  {
    role: 'assistant',
    senderName: '石雕匠人',
    senderId: 'fz_lacquerware',
    content: '软木雕刻讲究“刀法稳、层次清”，常用平刀、斜刀、圆刀等。代表作品如《松鹤图》，层次分明，细节生动。',
    streaming: true,
    time: '16:58',
  },
];

const DEMO_EVIDENCE = [
  {
    id: 'demo-e1',
    title: '国家级非遗名录 - 软木画',
    provider: '官方',
    confidence: 0.98,
    date: '2021-06-10',
    excerpt: '福州软木画以天然软木为材，雕刻与拼贴结合，具有极强的装饰性与审美价值。',
    type: '官方',
  },
  {
    id: 'demo-e2',
    title: '福州软木画工艺流程图解',
    provider: '图文',
    confidence: 0.93,
    date: '2019-08-21',
    excerpt: '工艺流程图显示制作包含选材、设计、雕刻、拼贴、打磨、上色等步骤。',
    type: '图文',
  },
  {
    id: 'demo-e3',
    title: '海上丝绸之路中的福州工艺',
    provider: '论文',
    confidence: 0.9,
    date: '2020-11-05',
    excerpt: '软木画在地方工艺交流中承载了外销与文化传播双重价值。',
    type: '论文',
  },
  {
    id: 'demo-e4',
    title: '软木画代表作品《松鹤图》',
    provider: '图片',
    confidence: 0.92,
    date: '2022-03-12',
    excerpt: '作品层次立体，兼具民间吉祥寓意与装饰美感。',
    type: '图片',
  },
  {
    id: 'demo-e5',
    title: '福州地方志·工艺美术卷',
    provider: '地方志',
    confidence: 0.89,
    date: '2018-05-30',
    excerpt: '地方志收录了软木画的历史沿革、传承脉络与社会应用。',
    type: '地方志',
  },
  {
    id: 'demo-e6',
    title: '软木材料特性与保存方法',
    provider: '论文',
    confidence: 0.88,
    date: '2021-09-17',
    excerpt: '软木材质轻、韧、易塑形，但需注意保存环境与长期维护。',
    type: '论文',
  },
];

const rolePalette = ['工艺专家', '材料专家', '历史学者', '民俗专家', '知识检索', '雕刻专家'];
const roleDescriptions = [
  '擅长软木画的工艺流程、材料特性与技法解析',
  '精通中国传统漆器工艺、材料与制作技法',
  '研究海洋丝绸之路历史、文物与文化交流',
  '熟悉纹样寓意、装饰结构与民间应用',
  '擅长非遗知识检索、图谱梳理与证据归纳',
  '专注刀法层次、材质塑形与代表作品解读',
];

const catalogAgents = ref([]);
const loadingAgents = ref(false);

const catalogAgentMap = computed(() => new Map(catalogAgents.value.map((agent) => [agent.agentCode, agent])));

function enrichAgent(agent, index = 0) {
  if (!agent) return null;
  const source = catalogAgentMap.value.get(agent.agentCode) || {};
  const model = toAgentViewModel({
    ...source,
    ...agent,
  });

  const fallbackIndex = index % rolePalette.length;

  return {
    ...model,
    id: agent.id ?? model.id ?? `agent-${agent.agentCode}-${index}`,
    agentCode: agent.agentCode,
    name: agent.name || model.name || '智能体',
    avatar: model.avatar || agent.avatar || '/logo.svg',
    tag: agent.tag || source.tag || rolePalette[fallbackIndex],
    description:
      agent.description ||
      source.description ||
      source.personality ||
      source.knowledgeScope ||
      roleDescriptions[fallbackIndex],
    official: true,
    online: true,
  };
}

const previewAgents = computed(() => roomChatAgents.value.map((agent, index) => enrichAgent(agent, index)).filter(Boolean));

const activeRoom = computed(() => rooms.value.find((room) => room.id === currentRoomId.value) || null);
const activeRoomLabel = computed(() => activeRoom.value?.name || '非遗主题群聊');

const currentRoomAgentCodes = computed(
  () => new Set(roomChatAgents.value.map((agent) => agent.agentCode).filter(Boolean)),
);

const availableAgentsToAdd = computed(() =>
  catalogAgents.value.filter((agent) => !currentRoomAgentCodes.value.has(agent.agentCode)),
);

const remainingRoomAgentSlots = computed(() => Math.max(0, 6 - roomChatAgents.value.length));

const selectedCount = computed(() => selectedAgentCodes.value.size);

const actionDisabled = computed(() => {
  if (loadingAgents.value) return true;
  if (roomManageMode.value) {
    return selectedCount.value < 1 || selectedCount.value > remainingRoomAgentSlots.value;
  }
  return selectedCount.value < 1 || selectedCount.value > 6;
});

const visibleMessages = computed(() => messages.value);

const latestAssistantMessage = computed(() => {
  return [...messages.value].reverse().find((message) => message.role === 'assistant') || null;
});

const latestAssistantMetadata = computed(() => latestAssistantMessage.value?.metadata || {});
const roomInsightConfidence = computed(() => roomInsight.value?.confidence || latestAssistantMetadata.value.confidence || {});
const roomInsightEvidenceSources = computed(() => roomInsight.value?.evidenceSources || []);
const roomInsightKnowledgeTags = computed(() => roomInsight.value?.knowledgeTags || []);
const roomInsightRelationPaths = computed(() => roomInsight.value?.relationPaths || []);

const evidenceSources = computed(() => {
  if (roomInsightEvidenceSources.value.length) {
    return roomInsightEvidenceSources.value;
  }

  const metadata = latestAssistantMetadata.value || {};
  const ragSources = Array.isArray(metadata.rag?.sources) ? metadata.rag.sources : [];
  const webSources = Array.isArray(metadata.webSearch?.sources) ? metadata.webSearch.sources : [];
  const combined = [
    ...ragSources.map((source, index) => ({
      id: source.id || `rag-${index}`,
      title: source.title || `RAG 证据 ${index + 1}`,
      provider: source.category || source.region || 'RAG',
      confidence: source.score != null ? Number(source.score) : 0,
      date: source.date || source.publishDate || source.metadata?.date || '',
      excerpt: source.excerpt || source.content || '',
      type: source.category || 'RAG',
      url: source.url || '',
    })),
    ...webSources.map((source, index) => ({
      id: source.url || `web-${index}`,
      title: source.title || `网页来源 ${index + 1}`,
      provider: source.url ? getHost(source.url) : '网页',
      confidence: source.score != null ? Number(source.score) : 0,
      date: source.date || '',
      excerpt: source.excerpt || source.content || '',
      type: '网页',
      url: source.url || '',
    })),
  ];

  return combined.slice(0, 6).map((item, index) => ({
    ...item,
    confidence: clamp01(item.confidence || Math.max(0.65, 0.92 - index * 0.04)),
  }));
});

const knowledgeGraphPills = computed(() => {
  if (roomInsightKnowledgeTags.value.length) {
    return roomInsightKnowledgeTags.value;
  }

  const pills = [
    activeRoom.value?.name,
    ...previewAgents.value.map((agent) => agent?.name).filter(Boolean),
    ...evidenceSources.value.slice(0, 2).map((source) => source.title).filter(Boolean),
  ];
  return Array.from(new Set(pills)).filter(Boolean).slice(0, 8);
});

const confidenceScore = computed(() => {
  const raw = Number(roomInsightConfidence.value.score || latestAssistantMetadata.value.confidence?.score);
  if (!Number.isFinite(raw) || raw <= 0) return 0;
  return raw > 1 ? clamp01(raw / 100) : clamp01(raw);
});

const confidenceLevelText = computed(() => {
  const level = roomInsightConfidence.value.level || latestAssistantMetadata.value.confidence?.level;
  if (level === 'high' || level === '高') return '高';
  if (level === 'medium' || level === '中') return '中';
  if (level === 'low' || level === '低') return '低';
  return confidenceScore.value > 0 ? '中' : '待生成';
});

const confidenceReason = computed(() => {
  const reason =
    roomInsight.value?.summary ||
    roomInsightConfidence.value.reason ||
    latestAssistantMetadata.value.confidence?.reason ||
    latestAssistantMetadata.value.webSearch?.summary;
  return reason || '发送问题后，右侧将自动汇总证据链、知识图谱和置信度。';
});

const roomHistoryItems = computed(() =>
  rooms.value.map((room) => ({
    id: room.id,
    title: room.name || `对话 ${room.id}`,
  })),
);

function clamp01(value) {
  return Math.min(1, Math.max(0, Number(value) || 0));
}

function getHost(url) {
  try {
    return new URL(url).host;
  } catch {
    return url || '';
  }
}

function formatTime(value) {
  if (!value) return '--:--';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '--:--';
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
}

function formatPercent(value) {
  const raw = Number(value);
  if (!Number.isFinite(raw)) return '0.00';
  return clamp01(raw > 1 ? raw / 100 : raw).toFixed(2);
}

function formatConfidenceBadge(value) {
  const raw = Number(value);
  if (!Number.isFinite(raw) || raw <= 0) return '置信度 --';
  const score = raw > 1 ? raw / 100 : raw;
  return `置信度 ${clamp01(score).toFixed(2)}`;
}

function messageKey(message, index) {
  return message.messageId || message.id || message.streamId || `${message.role}-${index}`;
}

function messageTime(message, index) {
  return message.time || message.timestamp || message.createTime || formatTime(Date.now() - index * 60000);
}

function getMessageAvatar(message, index) {
  if (message.role === 'user') return '';
  if (message.senderAvatar) return message.senderAvatar;
  const agent = previewAgents.value.find((item) => item.agentCode === message.senderId) || previewAgents.value[index];
  return agent?.avatar || '/logo.svg';
}

function getMessageTag(message, index) {
  if (message.role === 'user') return '提问者';
  const agent = previewAgents.value.find((item) => item.agentCode === message.senderId) || previewAgents.value[index];
  return agent?.tag || '智能体';
}

function getMessageStatus(message) {
  if (message.role === 'user') {
    return message.pending ? '发送中' : '已送达';
  }
  if (message.streaming || message.demo) {
    return '流式回复中';
  }
  return '已完成';
}

function getMessageContentClass(message) {
  if (message.role === 'user') return 'user-message';
  if (message.streaming || message.demo) return 'assistant-message assistant-streaming';
  return 'assistant-message';
}

function getConfidenceBadgeClass(value) {
  if (value >= 0.9) return 'badge badge-good';
  if (value >= 0.75) return 'badge badge-warm';
  return 'badge badge-cool';
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatScrollRef.value;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
  });
}

function resetSelection() {
  selectedAgentCodes.value = new Set();
}

function openAgentPicker(mode = 'create') {
  roomManageMode.value = mode === 'manage';
  selectedAgentCodes.value = new Set();
  agentPickerOpen.value = true;
}

function closeAgentPicker() {
  agentPickerOpen.value = false;
}

function openRoomAgentManage() {
  if (!currentRoomId.value) {
    openAgentPicker('create');
    return;
  }
  openAgentPicker('manage');
}

function handleUploadClick() {
  attachmentInput.value?.click();
}

function requestPageFullscreen() {
  document.documentElement.requestFullscreen?.();
}

async function handleAttachmentChange(event) {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;

  attachmentUploading.value = true;
  bootError.value = '';
  try {
    const uploaded = await chatApi.uploadImage(file);
    attachment.value = {
      name: file.name,
      type: file.type,
      url: uploaded?.url || '',
    };
  } catch (error) {
    attachment.value = null;
    bootError.value = error instanceof Error ? error.message : String(error);
  } finally {
    attachmentUploading.value = false;
  }
}

function clearAttachment() {
  attachment.value = null;
}

function handleSendMessage() {
  const text = inputText.value.trim();
  const imageUrl = attachment.value?.url || null;
  if (!text && !imageUrl) return;
  if (!currentRoomId.value) {
    bootError.value = '请先创建或选择一个群聊房间。';
    return;
  }
  if (attachmentUploading.value) {
    bootError.value = '图片仍在上传，请稍后发送。';
    return;
  }

  bootError.value = '';
  sendUserMessage(text, {
    searchEnabled: searchEnabled.value,
    ragEnabled: searchEnabled.value,
    respondAll: mentionAll.value,
    imageUrl,
  });
  inputText.value = '';
  clearAttachment();
  scrollToBottom();
}

function handleQuickCreate() {
  if (currentRoomId.value) {
    openAgentPicker('manage');
    return;
  }
  openAgentPicker('create');
}

function handleCreateChatRoom() {
  openAgentPicker('create');
}

async function handleOpenHistoricalRoom(roomId) {
  if (!roomId) return;
  bootError.value = '';
  try {
    await openRoom(roomId);
    closeAgentPicker();
    resetSelection();
  } catch (error) {
    bootError.value = error instanceof Error ? error.message : String(error);
  }
}

async function handleCreateGroupFromSelection() {
  const codes = Array.from(selectedAgentCodes.value);
  if (codes.length < 1 || codes.length > 6) {
    bootError.value = '创建群聊需要选择 1 到 6 个 Agent。';
    return;
  }

  bootError.value = '';
  try {
    await createNewRoom({
      name: `非遗群聊 ${new Date().toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      })}`,
      agentCodes: codes,
    });
    closeAgentPicker();
    resetSelection();
  } catch (error) {
    bootError.value = error instanceof Error ? error.message : String(error);
  }
}

async function handleAddSelectedAgentsToRoom() {
  const codes = Array.from(selectedAgentCodes.value);
  if (!currentRoomId.value) {
    bootError.value = '请先选择一个群聊房间。';
    return;
  }
  if (codes.length === 0) {
    bootError.value = '请选择要添加的 Agent。';
    return;
  }
  if (roomChatAgents.value.length + codes.length > 6) {
    bootError.value = '群聊最多保留 6 个 Agent。';
    return;
  }

  bootError.value = '';
  try {
    for (const code of codes) {
      await addRoomAgentToCurrentRoom(code);
    }
    closeAgentPicker();
    resetSelection();
  } catch (error) {
    bootError.value = error instanceof Error ? error.message : String(error);
  }
}

async function handleRemoveRoomAgent(agent) {
  if (!currentRoomId.value || !agent?.id) return;
  if (previewAgents.value.length <= 1) {
    bootError.value = '群聊至少保留 1 个 Agent。';
    return;
  }
  if (!window.confirm(`确定移除 ${agent.name || '这个 Agent'} 吗？`)) return;

  bootError.value = '';
  try {
    await removeRoomAgentFromCurrentRoom(agent.id);
    scheduleRoomInsightRefresh();
  } catch (error) {
    bootError.value = error instanceof Error ? error.message : String(error);
  }
}

function toggleAgentSelection(agent) {
  const code = agent.agentCode;
  if (!code) return;

  const next = new Set(selectedAgentCodes.value);
  const isSelected = next.has(code);
  if (isSelected) {
    next.delete(code);
    selectedAgentCodes.value = next;
    bootError.value = '';
    return;
  }

  const maxCount = roomManageMode.value ? remainingRoomAgentSlots.value : 6;
  if (next.size >= maxCount) {
    bootError.value = roomManageMode.value
      ? `当前群聊最多还能添加 ${maxCount} 个 Agent。`
      : '创建群聊最多选择 6 个 Agent。';
    return;
  }

  next.add(code);
  selectedAgentCodes.value = next;
  bootError.value = '';
}

async function bootstrap() {
  bootError.value = '';
  try {
    await loadRooms();
    const raw = localStorage.getItem(LAST_ROOM_KEY);
    const lastId = raw ? Number(raw) : Number.NaN;
    const exists = Number.isFinite(lastId) && rooms.value.some((room) => room.id === lastId);
    if (exists) {
      await openRoom(lastId);
      return;
    }
    if (rooms.value.length > 0) {
      await openRoom(rooms.value[0].id);
    }
  } catch (error) {
    bootError.value = error instanceof Error ? error.message : String(error);
  }
}

async function loadAgentCatalog() {
  loadingAgents.value = true;
  try {
    const list = await chatApi.fetchAgents();
    catalogAgents.value = Array.isArray(list) && list.length
      ? list.map((agent) => ({
          ...agent,
          description:
            agent.personality || agent.knowledgeScope || agent.roleType || '非遗文化主题智能体',
        }))
      : [];
  } catch (error) {
    catalogAgents.value = [];
    bootError.value = error instanceof Error ? error.message : String(error);
  } finally {
    loadingAgents.value = false;
  }
}

async function loadRoomInsight(roomId = currentRoomId.value) {
  if (!roomId) {
    roomInsight.value = null;
    return;
  }
  const requestedRoomId = roomId;
  loadingInsight.value = true;
  try {
    const insight = await chatApi.fetchRoomInsight(roomId);
    if (currentRoomId.value === requestedRoomId) {
      roomInsight.value = insight;
    }
  } catch {
    roomInsight.value = null;
  } finally {
    loadingInsight.value = false;
  }
}

function scheduleRoomInsightRefresh() {
  if (insightRefreshTimer) {
    window.clearTimeout(insightRefreshTimer);
  }
  if (!currentRoomId.value) {
    roomInsight.value = null;
    return;
  }
  insightRefreshTimer = window.setTimeout(() => {
    loadRoomInsight(currentRoomId.value);
  }, 280);
}

watch(currentRoomId, (roomId) => {
  if (roomId != null) {
    localStorage.setItem(LAST_ROOM_KEY, String(roomId));
  } else {
    localStorage.removeItem(LAST_ROOM_KEY);
  }
  scheduleRoomInsightRefresh();
});

watch(
  () => [messages.value.length, roomChatAgents.value.length, currentRoomId.value],
  () => scrollToBottom(),
);

watch(
  () => chatRevision.value,
  () => {
    scrollToBottom();
    scheduleRoomInsightRefresh();
  },
);

watch(
  () => route.path,
  () => {
    agentPickerOpen.value = false;
  },
);

onMounted(async () => {
  await Promise.all([loadAgentCatalog(), bootstrap()]);
  scheduleRoomInsightRefresh();
  scrollToBottom();
});

onBeforeUnmount(() => {
  if (insightRefreshTimer) {
    window.clearTimeout(insightRefreshTimer);
  }
});
</script>

<template>
  <main class="agent-console">
    <div class="agent-stars" aria-hidden="true"></div>
    <div class="agent-gridline" aria-hidden="true"></div>
    <div class="agent-wave" aria-hidden="true">
      <svg viewBox="0 0 1600 120" preserveAspectRatio="none">
        <path d="M0,70 C160,20 320,20 480,70 C640,120 800,120 960,70 C1120,20 1280,20 1440,70 C1520,95 1560,95 1600,70" />
        <path d="M0,84 C180,52 360,52 540,84 C720,116 900,116 1080,84 C1260,52 1440,52 1600,84" />
      </svg>
    </div>

    <header class="agent-hero">
      <div class="hero-spacer" aria-hidden="true"></div>

      <div class="hero-copy">
        <h1>多智能体群聊协作台</h1>
        <p>{{ currentRoomId ? `当前房间：${activeRoomLabel}` : '创建或选择群聊后开始协作问答' }}</p>
      </div>

      <div class="hero-actions">
        <div class="mode-pill">
          <ChatBubbleLeftRightIcon class="icon-sm" />
          <span>文化问答</span>
        </div>
        <div class="mode-pill">
          <BookOpenIcon class="icon-sm" />
          <span>知识增强</span>
        </div>
        <div class="mode-pill">
          <SparklesIcon class="icon-sm" />
          <span>流式回复</span>
        </div>
        <button class="fullscreen-btn" type="button" title="全屏" @click="requestPageFullscreen">
          <ArrowsPointingOutIcon class="icon-md" />
        </button>
      </div>
    </header>

    <section class="workspace">
      <aside class="panel members-panel">
        <div class="panel-head">
          <div class="panel-title">
            <UsersIcon class="icon-md" />
            <span>群聊成员 ({{ previewAgents.length }}/6)</span>
          </div>
          <button class="ghost-btn" type="button" @click="openRoomAgentManage">
            <PlusIcon class="icon-sm" />
            <span>添加 Agent</span>
          </button>
        </div>

        <div class="member-list scrollbar-track-transparent">
          <div v-if="previewAgents.length === 0" class="empty-card">
            <UsersIcon class="icon-md" />
            <span>{{ currentRoomId ? '当前群聊还没有 Agent 成员' : '请选择或创建一个群聊' }}</span>
          </div>
          <template v-else>
            <article
              v-for="(agent, index) in previewAgents"
              :key="agent.agentCode || agent.id || index"
              class="member-card"
            >
              <img
                class="member-avatar"
                :src="agent.avatar || '/logo.svg'"
                :alt="agent.name"
                @error="(event) => { event.target.src = '/logo.svg'; }"
              />
              <div class="member-copy">
                <div class="member-topline">
                  <div class="member-name">{{ agent.name }}</div>
                  <span class="member-status">
                    <span class="status-dot"></span>
                    在线
                  </span>
                  <button
                    class="member-remove-btn"
                    type="button"
                    :disabled="previewAgents.length <= 1"
                    :title="previewAgents.length <= 1 ? '群聊至少保留 1 个 Agent' : '移除 Agent'"
                    @click="handleRemoveRoomAgent(agent)"
                  >
                    <TrashIcon class="icon-xs" />
                  </button>
                </div>
                <p class="member-desc">{{ agent.description }}</p>
              </div>
            </article>
          </template>
        </div>

        <button class="invite-btn" type="button" @click="handleQuickCreate">
          <UserPlusIcon class="icon-md" />
          <span>邀请更多 Agent</span>
        </button>
      </aside>

      <section class="panel chat-panel">
        <header class="chat-head">
          <div class="chat-head-left">
            <div class="panel-title">
              <ChatBubbleLeftRightIcon class="icon-md" />
              <span>群聊对话 · 非遗主题</span>
              <span class="live-badge">
                <SparklesIcon class="icon-xs" />
                直播中
              </span>
            </div>
            <p class="chat-subtitle">围绕同一非遗主题，由多位 Agent 分工协作回答。</p>
          </div>

          <div class="chat-head-right">
            <button class="new-room-btn" type="button" @click="handleCreateChatRoom">
              <PlusIcon class="icon-sm" />
              <span>新建聊天室</span>
            </button>
          </div>
        </header>

        <div v-if="bootError || wsError" class="notice-strip">
          {{ bootError || wsError }}
        </div>

        <div ref="chatScrollRef" class="chat-scroll scrollbar-track-transparent">
          <div v-if="loadingMessages" class="state-block">正在同步群聊记录...</div>
          <template v-else>
            <section class="message-stack">
              <template v-for="(message, index) in visibleMessages" :key="messageKey(message, index)">
                <div v-if="message.role === 'system'" class="system-row">
                  <div class="system-pill">
                    {{ message.content }}
                    <span v-if="message.onlineCount != null" class="system-muted">（在线 {{ message.onlineCount }}）</span>
                  </div>
                </div>

                <article
                  v-else-if="message.role === 'user'"
                  class="message-card user-card"
                >
                  <div class="message-meta user-meta">
                    <div class="meta-left">
                      <span class="meta-name">{{ message.senderName || '用户' }}</span>
                      <span class="meta-tag">提问者</span>
                    </div>
                    <div class="meta-right">
                      <ClockIcon class="icon-xs" />
                      <span>{{ messageTime(message, index) }}</span>
                      <span class="meta-state">{{ getMessageStatus(message) }}</span>
                    </div>
                  </div>
                  <div v-if="message.imageUrl" class="message-image">
                    <img :src="message.imageUrl" alt="上传图片" />
                  </div>
                  <p class="message-text">{{ message.content }}</p>
                </article>

                <article
                  v-else
                  class="message-card assistant-card"
                  :class="getMessageContentClass(message)"
                >
                  <div class="assistant-avatar-wrap">
                    <img
                      class="assistant-avatar"
                      :src="getMessageAvatar(message, index) || '/logo.svg'"
                      :alt="message.senderName || 'Agent'"
                      @error="(event) => { event.target.src = '/logo.svg'; }"
                    />
                  </div>

                  <div class="assistant-body">
                    <div class="message-meta assistant-meta">
                      <div class="meta-left">
                        <span class="meta-name">{{ message.senderName || 'Agent' }}</span>
                        <span v-if="message.streaming || message.demo" class="meta-live">正在回复...</span>
                      </div>
                      <div class="meta-right">
                        <ClockIcon class="icon-xs" />
                        <span>{{ messageTime(message, index) }}</span>
                        <span class="meta-state meta-state-live">{{ getMessageStatus(message) }}</span>
                      </div>
                    </div>

                    <div v-if="message.imageUrl" class="message-image assistant-image">
                      <img :src="message.imageUrl" alt="Agent 图片" />
                    </div>

                    <p class="message-text">{{ message.content }}</p>

                    <div
                      v-if="message.metadata?.confidence?.level || message.metadata?.rag?.sources?.length || message.metadata?.webSearch?.sources?.length"
                      class="mini-evidence"
                    >
                      <div class="mini-evidence-head">
                        <ShieldCheckIcon class="icon-xs" />
                        <span>证据摘要</span>
                        <span class="mini-evidence-score">
                          {{ formatConfidenceBadge(message.metadata?.confidence?.score) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </article>
              </template>
            </section>
          </template>
        </div>

        <footer class="composer-shell">
          <div class="composer">
            <textarea
              v-model="inputText"
              class="composer-input scrollbar-track-transparent"
              rows="1"
              :disabled="!currentRoomId || loadingMessages || !wsConnected"
              :placeholder="currentRoomId ? '输入您想了解的非遗问题，或 @ 某个 Agent 提问...' : '请先创建或选择一个群聊房间'"
              @keydown.enter.exact.prevent="handleSendMessage"
            />

            <div v-if="attachment" class="attachment-chip">
              <PhotoIcon class="icon-xs" />
              <span>{{ attachment.name }}</span>
              <button class="chip-close" type="button" @click="clearAttachment">
                <XMarkIcon class="icon-xs" />
              </button>
            </div>

            <div class="composer-actions">
              <button
                class="chip-btn"
                :class="{ active: mentionAll }"
                type="button"
                :disabled="!currentRoomId || loadingMessages || !wsConnected"
                @click="mentionAll = !mentionAll"
              >
                <AtSymbolIcon class="icon-xs" />
                <span>所有人</span>
              </button>

              <button
                class="chip-btn"
                type="button"
                :disabled="!currentRoomId || loadingMessages || !wsConnected"
                @click="handleUploadClick"
              >
                <PhotoIcon class="icon-xs" />
                <span>上传图片</span>
              </button>

              <button
                class="chip-btn"
                :class="{ active: searchEnabled }"
                type="button"
                :disabled="!currentRoomId || loadingMessages || !wsConnected"
                @click="searchEnabled = !searchEnabled"
              >
                <MagnifyingGlassIcon class="icon-xs" />
                <span>知识增强</span>
              </button>

              <button
                class="send-btn"
                type="button"
                :disabled="!currentRoomId || loadingMessages || !wsConnected || !inputText.trim()"
                @click="handleSendMessage"
              >
                <PaperAirplaneIcon class="icon-sm" />
              </button>
            </div>
          </div>
          <p class="composer-footnote">所有回复由多智能体协作生成，内容仅供参考</p>
        </footer>
      </section>

      <aside class="panel insight-panel">
        <div class="panel-head">
          <div class="panel-title">
            <CubeTransparentIcon class="icon-md" />
            <span>知识增强与证据链</span>
            <span v-if="loadingInsight" class="live-badge">
              <SparklesIcon class="icon-xs" />
              生成中
            </span>
          </div>
        </div>

        <div class="insight-tabs">
          <button
            class="tab-btn"
            :class="{ active: insightTab === 'evidence' }"
            type="button"
            @click="insightTab = 'evidence'"
          >
            证据链
          </button>
          <button
            class="tab-btn"
            :class="{ active: insightTab === 'graph' }"
            type="button"
            @click="insightTab = 'graph'"
          >
            知识图谱
          </button>
          <button
            class="tab-btn"
            :class="{ active: insightTab === 'sources' }"
            type="button"
            @click="insightTab = 'sources'"
          >
            溯源信息
          </button>
        </div>

        <div class="insight-scroll scrollbar-track-transparent">
          <section v-if="insightTab === 'evidence'" class="insight-block">
            <div class="summary-card">
              <div class="section-head">
                <div class="section-title">
                  <DocumentTextIcon class="icon-sm" />
                  <span>知识摘要</span>
                </div>
              </div>
              <p v-if="loadingInsight" class="summary-text">正在生成知识摘要与证据链…</p>
              <p v-else class="summary-text">
                {{ confidenceReason }}
              </p>
            </div>

            <div class="section-card">
              <div class="section-head">
                <div class="section-title">
                  <CircleStackIcon class="icon-sm" />
                  <span>关键证据 ({{ evidenceSources.length }})</span>
                </div>
              </div>

              <div v-if="evidenceSources.length" class="evidence-list">
                <article
                  v-for="(source, index) in evidenceSources"
                  :key="source.id || index"
                  class="evidence-item"
                >
                  <div class="evidence-index">{{ index + 1 }}</div>
                  <div class="evidence-copy">
                    <div class="evidence-topline">
                      <span class="evidence-title">{{ source.title }}</span>
                      <span class="evidence-provider">{{ source.provider }}</span>
                    </div>
                    <div class="evidence-meta">
                      <span class="evidence-score">置信度 {{ formatPercent(source.confidence) }}</span>
                      <span v-if="source.date">{{ source.date }}</span>
                    </div>
                    <p v-if="source.excerpt" class="evidence-excerpt">{{ source.excerpt }}</p>
                  </div>
                </article>
              </div>
              <div v-else class="empty-state compact">
                暂无可展示证据，发送问题后会自动检索并汇总。
              </div>
            </div>

            <div class="section-card confidence-card">
              <div class="section-head">
                <div class="section-title">
                  <ShieldCheckIcon class="icon-sm" />
                  <span>综合置信度</span>
                </div>
                <div class="confidence-value">{{ formatPercent(confidenceScore) }}</div>
              </div>

              <div class="confidence-meter">
                <div class="confidence-fill" :style="{ width: `${confidenceScore * 100}%` }"></div>
              </div>
              <div class="confidence-foot">
                <span :class="getConfidenceBadgeClass(confidenceScore)">{{ confidenceLevelText }}</span>
                <span>{{ confidenceScore > 0 ? '基于多条证据与多智能体一致性评估' : '等待新的问题生成评估结果' }}</span>
              </div>
            </div>
          </section>

          <section v-else-if="insightTab === 'graph'" class="insight-block">
            <div class="section-card">
              <div class="section-head">
                <div class="section-title">
                  <GlobeAltIcon class="icon-sm" />
                  <span>知识图谱</span>
                </div>
              </div>
              <div class="pill-cloud">
                <span v-for="pill in knowledgeGraphPills" :key="pill" class="graph-pill">{{ pill }}</span>
              </div>
            </div>
            <div class="section-card">
              <div class="section-head">
                <div class="section-title">
                  <LinkIcon class="icon-sm" />
                  <span>关联路径</span>
                </div>
              </div>
              <div v-if="roomInsightRelationPaths.length" class="path-list">
                <div v-for="path in roomInsightRelationPaths" :key="`${path.key}-${path.value}`" class="path-row">
                  <span class="path-key">{{ path.key }}</span>
                  <span class="path-value">{{ path.value }}</span>
                </div>
              </div>
              <div v-else class="empty-state compact">
                发送问题后会自动生成主题、提问、回复与证据路径。
              </div>
            </div>
          </section>

          <section v-else class="insight-block">
            <div class="section-card">
              <div class="section-head">
                <div class="section-title">
                  <ListBulletIcon class="icon-sm" />
                  <span>溯源信息</span>
                </div>
              </div>
              <div v-if="evidenceSources.length" class="source-list">
                <article
                  v-for="source in evidenceSources"
                  :key="`${source.id}-src`"
                  class="source-row"
                >
                  <div class="source-title">{{ source.title }}</div>
                  <div class="source-subline">
                    <span>{{ source.provider }}</span>
                    <span>{{ formatPercent(source.confidence) }}</span>
                  </div>
                </article>
              </div>
              <div v-else class="empty-state compact">
                暂无溯源信息。
              </div>
            </div>
          </section>
        </div>
      </aside>
    </section>

    <div
      v-if="agentPickerOpen"
      class="modal-mask"
      @click.self="closeAgentPicker"
    >
      <section class="agent-picker">
        <header class="picker-head">
          <div>
            <div class="picker-title">
              <UsersIcon class="icon-md" />
              <span>{{ roomManageMode ? '添加 Agent' : '创建群聊' }}</span>
            </div>
            <p class="picker-subtitle">
              {{ roomManageMode ? '从知识库中选择可加入当前群聊的 Agent，最多仍保留 6 个。' : '选择 1 到 6 个 Agent 组成新的多智能体群聊。' }}
            </p>
          </div>
          <button class="icon-btn" type="button" @click="closeAgentPicker">
            <XMarkIcon class="icon-sm" />
          </button>
        </header>

        <div class="picker-summary">
          <div class="picker-summary-item">
            <span class="summary-key">当前房间</span>
            <span class="summary-val">{{ activeRoomLabel }}</span>
          </div>
          <div class="picker-summary-item">
            <span class="summary-key">已选数量</span>
            <span class="summary-val">{{ selectedCount }}/{{ roomManageMode ? remainingRoomAgentSlots : 6 }}</span>
          </div>
        </div>

        <div v-if="!roomManageMode" class="history-panel">
          <div class="section-head">
            <div class="section-title">
              <ClockIcon class="icon-sm" />
              <span>历史聊天室</span>
            </div>
            <span class="history-hint">点击可直接打开</span>
          </div>
          <div v-if="roomHistoryItems.length" class="history-list">
            <button
              v-for="room in roomHistoryItems"
              :key="room.id"
              class="history-room"
              :class="{ active: room.id === currentRoomId }"
              type="button"
              @click="handleOpenHistoricalRoom(room.id)"
            >
              <span class="history-room-title">{{ room.title }}</span>
              <span class="history-room-meta">{{ room.id === currentRoomId ? '当前' : '历史' }}</span>
            </button>
          </div>
          <div v-else class="empty-state compact">
            暂无历史聊天室。
          </div>
        </div>

        <div class="picker-grid">
          <button
            v-for="(agent, index) in roomManageMode ? availableAgentsToAdd : catalogAgents"
            :key="agent.agentCode || agent.id || index"
            class="picker-card"
            :class="{ selected: selectedAgentCodes.has(agent.agentCode) }"
            type="button"
            @click="toggleAgentSelection(agent)"
          >
            <img class="picker-avatar" :src="toAgentViewModel(agent).avatar || '/logo.svg'" :alt="agent.name" />
            <div class="picker-copy">
              <div class="picker-row">
                <span class="picker-name">{{ agent.name }}</span>
                <span class="picker-tag">{{ agent.tag || rolePalette[index % rolePalette.length] }}</span>
              </div>
              <p class="picker-desc">{{ agent.description }}</p>
            </div>
            <div class="picker-flag" :class="{ visible: selectedAgentCodes.has(agent.agentCode) }">
              <CheckCircleIcon class="icon-sm" />
              <span>已选</span>
            </div>
          </button>
        </div>

        <div class="picker-foot">
          <div class="picker-hint">
            <CpuChipIcon class="icon-sm" />
            <span>{{ roomManageMode ? '选择后将直接加入当前群聊' : '选定的 Agent 将组成新的群聊组合' }}</span>
          </div>

          <div class="picker-actions">
            <button class="secondary-btn" type="button" @click="closeAgentPicker">取消</button>
            <button
              class="primary-btn"
              type="button"
              :disabled="actionDisabled"
              @click="roomManageMode ? handleAddSelectedAgentsToRoom() : handleCreateGroupFromSelection()"
            >
              {{ roomManageMode ? `添加 ${selectedCount} 个 Agent` : `创建群聊（${selectedCount}/6）` }}
            </button>
          </div>
        </div>
      </section>
    </div>

    <input
      ref="attachmentInput"
      type="file"
      accept="image/*"
      class="sr-only"
      @change="handleAttachmentChange"
    />
  </main>
</template>

<script>
export default {
  name: 'AgentPage',
};
</script>

<style scoped>
.agent-console {
  position: relative;
  min-height: 100dvh;
  overflow: hidden;
  padding-top: 64px;
  background:
    radial-gradient(circle at 50% 0%, rgba(44, 103, 255, 0.18), transparent 34%),
    linear-gradient(180deg, #02111f 0%, #020814 50%, #01050d 100%);
  color: #eaf6ff;
}

.agent-stars,
.agent-gridline,
.agent-wave {
  pointer-events: none;
  position: absolute;
  inset: 0;
}

.agent-stars {
  background-image:
    radial-gradient(circle at 12% 18%, rgba(120, 208, 255, 0.7) 0 1px, transparent 1.5px),
    radial-gradient(circle at 28% 32%, rgba(105, 231, 198, 0.6) 0 1px, transparent 1.4px),
    radial-gradient(circle at 43% 11%, rgba(173, 122, 255, 0.65) 0 1px, transparent 1.5px),
    radial-gradient(circle at 64% 19%, rgba(110, 193, 255, 0.7) 0 1px, transparent 1.4px),
    radial-gradient(circle at 78% 27%, rgba(255, 255, 255, 0.6) 0 1px, transparent 1.5px),
    radial-gradient(circle at 87% 14%, rgba(80, 203, 255, 0.6) 0 1px, transparent 1.4px),
    radial-gradient(circle at 91% 42%, rgba(255, 255, 255, 0.45) 0 1px, transparent 1.4px),
    radial-gradient(circle at 19% 72%, rgba(110, 193, 255, 0.45) 0 1px, transparent 1.4px),
    radial-gradient(circle at 55% 82%, rgba(76, 255, 199, 0.4) 0 1px, transparent 1.4px),
    radial-gradient(circle at 74% 73%, rgba(255, 255, 255, 0.4) 0 1px, transparent 1.4px);
  opacity: 0.9;
}

.agent-gridline {
  background-image:
    linear-gradient(to right, rgba(67, 135, 255, 0.08) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(67, 135, 255, 0.06) 1px, transparent 1px);
  background-size: 160px 160px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.75), transparent 92%);
  opacity: 0.35;
}

.agent-wave {
  top: 0;
  height: 180px;
  opacity: 0.55;
}

.agent-wave svg {
  width: 100%;
  height: 100%;
  display: block;
}

.agent-wave path {
  fill: none;
  stroke: rgba(60, 172, 255, 0.45);
  stroke-width: 1.2;
  stroke-linecap: round;
}

.agent-wave path + path {
  stroke: rgba(162, 99, 255, 0.35);
}

.agent-hero {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  min-height: 94px;
  padding: 12px 18px 8px;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-self: start;
}

.brand-logo {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-name {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0;
}

.brand-sub {
  color: rgba(233, 245, 255, 0.82);
  font-size: 14px;
}

.hero-copy {
  justify-self: center;
  text-align: center;
  text-shadow: 0 0 22px rgba(45, 164, 255, 0.18);
}

.hero-copy h1 {
  margin: 0;
  font-size: 40px;
  font-weight: 800;
  line-height: 1.06;
  letter-spacing: 0;
}

.hero-copy p {
  margin: 8px 0 0;
  color: rgba(208, 230, 255, 0.68);
  font-size: 16px;
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-self: end;
  gap: 10px;
}

.mode-pill,
.fullscreen-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  border: 1px solid rgba(92, 177, 255, 0.28);
  border-radius: 8px;
  background: rgba(8, 23, 48, 0.62);
  padding: 0 12px;
  color: rgba(232, 246, 255, 0.92);
  font-size: 13px;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.mode-pill {
  cursor: default;
}

.fullscreen-btn:hover {
  border-color: rgba(140, 220, 255, 0.7);
  background: rgba(20, 53, 101, 0.72);
  color: #fff;
  transform: translateY(-1px);
}

.fullscreen-btn {
  width: 38px;
  justify-content: center;
  padding: 0;
}

.workspace {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(330px, 0.95fr) minmax(0, 1.45fr) minmax(330px, 0.92fr);
  gap: 12px;
  height: calc(100dvh - 166px);
  padding: 0 18px 18px;
}

.panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid rgba(72, 146, 255, 0.28);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(8, 18, 39, 0.86), rgba(4, 12, 27, 0.94)),
    rgba(4, 10, 24, 0.88);
  box-shadow:
    inset 0 0 0 1px rgba(120, 190, 255, 0.05),
    0 18px 46px rgba(1, 5, 14, 0.45);
  backdrop-filter: blur(14px);
}

.members-panel {
  padding: 14px 12px 12px;
}

.chat-panel {
  position: relative;
  padding: 14px 14px 12px;
}

.insight-panel {
  padding: 14px 12px 12px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #f3fbff;
  font-size: 18px;
  font-weight: 700;
}

.ghost-btn,
.icon-btn,
.chip-btn,
.send-btn,
.secondary-btn,
.primary-btn,
.tab-btn,
.room-menu-item,
.room-menu-action {
  border-radius: 8px;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease, transform 0.18s ease, opacity 0.18s ease;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  border: 1px solid rgba(123, 184, 255, 0.22);
  background: rgba(13, 25, 44, 0.58);
  padding: 0 12px;
  color: rgba(235, 247, 255, 0.86);
  font-size: 13px;
}

.ghost-btn:hover {
  background: rgba(23, 49, 89, 0.72);
  color: #fff;
}

.member-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}

.member-card {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 10px;
  padding: 12px 10px;
  margin-bottom: 10px;
  border: 1px solid rgba(101, 170, 255, 0.18);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(15, 29, 53, 0.64), rgba(10, 19, 38, 0.88));
  box-shadow: inset 0 0 0 1px rgba(148, 208, 255, 0.03);
}

.member-avatar,
.assistant-avatar,
.picker-avatar {
  object-fit: cover;
}

.member-avatar {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  border: 1px solid rgba(160, 214, 255, 0.16);
}

.member-copy {
  min-width: 0;
}

.member-topline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.member-name {
  min-width: 0;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.member-tag {
  border: 1px solid rgba(171, 126, 255, 0.35);
  border-radius: 999px;
  background: rgba(98, 66, 182, 0.28);
  padding: 2px 8px;
  color: #d8c7ff;
  font-size: 12px;
}

.member-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-left: auto;
  color: #6cffc8;
  font-size: 12px;
}

.member-remove-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid rgba(255, 125, 125, 0.2);
  border-radius: 8px;
  background: rgba(38, 18, 26, 0.78);
  color: #ffb3b3;
}

.member-remove-btn:hover:not(:disabled) {
  border-color: rgba(255, 138, 138, 0.36);
  background: rgba(72, 24, 32, 0.94);
  color: #fff;
}

.member-remove-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #32ff9a;
  box-shadow: 0 0 10px rgba(56, 255, 181, 0.75);
}

.member-desc {
  margin: 0;
  color: rgba(223, 238, 255, 0.66);
  font-size: 14px;
  line-height: 1.55;
}

.invite-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  margin-top: 10px;
  border: 1px solid rgba(127, 180, 255, 0.16);
  border-radius: 8px;
  background: rgba(11, 21, 38, 0.7);
  color: rgba(227, 242, 255, 0.9);
  font-size: 15px;
  font-weight: 600;
}

.invite-btn:hover {
  background: rgba(21, 39, 67, 0.88);
}

.chat-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.chat-head-left {
  min-width: 0;
}

.chat-subtitle {
  margin: 8px 0 0;
  color: rgba(219, 235, 255, 0.62);
  font-size: 13px;
  line-height: 1.45;
}

.chat-head-right {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
}

.new-room-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  border: 1px solid rgba(120, 186, 255, 0.24);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(24, 48, 86, 0.95), rgba(12, 24, 44, 0.96));
  padding: 0 14px;
  color: #f3fbff;
  font-size: 13px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(145, 210, 255, 0.04);
}

.new-room-btn:hover {
  border-color: rgba(144, 224, 255, 0.42);
  background: linear-gradient(180deg, rgba(31, 64, 112, 0.98), rgba(16, 31, 56, 0.98));
}

.live-badge,
.online-pill,
.meta-tag,
.meta-state,
.meta-live,
.evidence-provider,
.evidence-score,
.confidence-foot .badge,
.member-tag,
.picker-tag {
  white-space: nowrap;
}

.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 24px;
  margin-left: 6px;
  border: 1px solid rgba(160, 119, 255, 0.36);
  border-radius: 999px;
  background: rgba(91, 58, 170, 0.28);
  padding: 0 8px;
  color: #c9adff;
  font-size: 12px;
}

.online-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  border: 1px solid rgba(90, 185, 255, 0.2);
  border-radius: 999px;
  background: rgba(9, 22, 39, 0.72);
  padding: 0 10px;
  color: rgba(214, 242, 255, 0.9);
  font-size: 12px;
}

.toolbar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(113, 175, 255, 0.18);
  background: rgba(8, 18, 34, 0.72);
  color: rgba(226, 242, 255, 0.9);
}

.icon-btn:hover,
.tab-btn:hover,
.chip-btn:hover,
.secondary-btn:hover {
  background: rgba(26, 48, 84, 0.92);
  color: #fff;
}

.room-menu {
  position: absolute;
  top: 42px;
  right: 0;
  z-index: 5;
  width: 260px;
  border: 1px solid rgba(98, 168, 255, 0.24);
  border-radius: 8px;
  background: rgba(5, 15, 31, 0.98);
  padding: 8px;
  box-shadow: 0 24px 50px rgba(0, 0, 0, 0.35);
}

.room-menu-action,
.room-menu-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  background: transparent;
  padding: 8px 10px;
  color: rgba(233, 247, 255, 0.9);
  font-size: 13px;
  text-align: left;
}

.room-menu-action {
  margin-bottom: 6px;
  border-color: rgba(109, 179, 255, 0.18);
  background: rgba(15, 32, 55, 0.7);
}

.room-menu-item:hover,
.room-menu-action:hover {
  background: rgba(24, 50, 86, 0.95);
}

.room-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #49d8ff;
  box-shadow: 0 0 10px rgba(58, 202, 255, 0.7);
}

.room-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-menu-empty {
  padding: 8px 10px 4px;
  color: rgba(206, 229, 255, 0.44);
  font-size: 12px;
}

.notice-strip {
  margin-bottom: 10px;
  border: 1px solid rgba(255, 196, 79, 0.24);
  border-radius: 8px;
  background: rgba(112, 77, 0, 0.18);
  padding: 8px 12px;
  color: #ffe2a7;
  font-size: 13px;
}

.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
}

.state-block {
  display: flex;
  min-height: 100%;
  align-items: center;
  justify-content: center;
  color: rgba(219, 236, 255, 0.52);
  font-size: 15px;
}

.message-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 2px 8px;
}

.system-row {
  display: flex;
  justify-content: center;
}

.system-pill {
  max-width: 100%;
  border: 1px solid rgba(148, 204, 255, 0.12);
  border-radius: 999px;
  background: rgba(10, 22, 38, 0.74);
  padding: 8px 14px;
  color: rgba(224, 239, 255, 0.6);
  font-size: 12px;
  line-height: 1.45;
  text-align: center;
}

.system-muted {
  color: rgba(224, 239, 255, 0.42);
}

.message-card {
  position: relative;
  display: grid;
  gap: 10px;
  border: 1px solid rgba(116, 177, 255, 0.24);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(16, 29, 61, 0.68), rgba(11, 18, 34, 0.92));
  padding: 12px 14px 14px;
  box-shadow: inset 0 0 0 1px rgba(160, 214, 255, 0.03);
}

.user-card {
  margin-left: auto;
  width: min(100%, 660px);
  border-color: rgba(72, 148, 255, 0.22);
  background: linear-gradient(180deg, rgba(22, 45, 91, 0.72), rgba(11, 22, 46, 0.92));
}

.assistant-card {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 12px;
  width: min(100%, 100%);
}

.assistant-streaming {
  border-color: rgba(137, 101, 255, 0.34);
  background: linear-gradient(180deg, rgba(46, 35, 92, 0.75), rgba(13, 17, 38, 0.92));
}

.assistant-avatar-wrap {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.assistant-avatar {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  border: 1px solid rgba(166, 208, 255, 0.18);
}

.assistant-body {
  min-width: 0;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.meta-left,
.meta-right {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.meta-name {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.meta-tag {
  border: 1px solid rgba(167, 129, 255, 0.38);
  border-radius: 999px;
  background: rgba(100, 69, 183, 0.2);
  padding: 2px 8px;
  color: #dbd1ff;
  font-size: 12px;
}

.meta-live {
  border: 1px solid rgba(89, 197, 255, 0.24);
  border-radius: 999px;
  background: rgba(33, 71, 119, 0.24);
  padding: 2px 8px;
  color: #7fd8ff;
  font-size: 12px;
}

.meta-right {
  color: rgba(217, 233, 255, 0.56);
  font-size: 12px;
}

.meta-state {
  border: 1px solid rgba(109, 178, 255, 0.18);
  border-radius: 999px;
  background: rgba(12, 25, 46, 0.78);
  padding: 2px 8px;
}

.meta-state-live {
  color: #65d9ff;
}

.message-text {
  margin: 0;
  color: rgba(236, 246, 255, 0.9);
  font-size: 15px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.message-image {
  margin-bottom: 10px;
  overflow: hidden;
  border: 1px solid rgba(146, 204, 255, 0.14);
  border-radius: 8px;
  background: rgba(10, 18, 31, 0.85);
}

.message-image img {
  display: block;
  width: 100%;
  max-height: 260px;
  object-fit: cover;
}

.mini-evidence {
  margin-top: 10px;
  border-top: 1px solid rgba(136, 195, 255, 0.1);
  padding-top: 10px;
}

.mini-evidence-head {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgba(216, 241, 255, 0.78);
  font-size: 12px;
}

.mini-evidence-score {
  border: 1px solid rgba(103, 173, 255, 0.2);
  border-radius: 999px;
  background: rgba(15, 30, 50, 0.76);
  padding: 2px 8px;
  color: rgba(214, 242, 255, 0.8);
}

.composer-shell {
  margin-top: 12px;
  padding-top: 2px;
}

.composer {
  border: 1px solid rgba(89, 162, 255, 0.22);
  border-radius: 8px;
  background: rgba(7, 16, 31, 0.86);
  padding: 10px;
}

.composer-input {
  width: 100%;
  min-height: 84px;
  max-height: 144px;
  resize: none;
  border: 0;
  background: transparent;
  color: #f6fbff;
  font-size: 15px;
  line-height: 1.65;
  outline: none;
}

.composer-input::placeholder {
  color: rgba(214, 233, 255, 0.42);
}

.composer-input:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

.attachment-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  border: 1px solid rgba(108, 176, 255, 0.2);
  border-radius: 999px;
  background: rgba(15, 29, 53, 0.9);
  padding: 4px 8px;
  color: rgba(234, 246, 255, 0.88);
  font-size: 12px;
}

.chip-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: rgba(225, 240, 255, 0.68);
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
}

.chip-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  border: 1px solid rgba(108, 178, 255, 0.14);
  background: rgba(11, 20, 34, 0.78);
  padding: 0 10px;
  color: rgba(229, 244, 255, 0.84);
  font-size: 12px;
}

.chip-btn.active {
  border-color: rgba(111, 218, 255, 0.34);
  background: rgba(18, 52, 86, 0.96);
  color: #fff;
}

.send-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: 0;
  background: linear-gradient(180deg, #7388ff 0%, #5368f5 100%);
  color: #fff;
  box-shadow: 0 10px 24px rgba(84, 103, 255, 0.26);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.send-btn:disabled,
.chip-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.composer-footnote {
  margin: 12px 0 0;
  color: rgba(210, 228, 255, 0.42);
  font-size: 12px;
  text-align: center;
}

.insight-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.tab-btn {
  height: 38px;
  border: 1px solid rgba(102, 166, 255, 0.18);
  background: rgba(9, 18, 32, 0.8);
  color: rgba(227, 240, 255, 0.72);
  font-size: 13px;
}

.tab-btn.active {
  border-color: rgba(112, 188, 255, 0.42);
  background: rgba(20, 46, 82, 0.96);
  color: #fff;
}

.insight-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.insight-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-card,
.section-card {
  border: 1px solid rgba(94, 165, 255, 0.16);
  border-radius: 8px;
  background: rgba(9, 18, 32, 0.82);
  padding: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.section-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #f2fbff;
  font-size: 15px;
  font-weight: 700;
}

.summary-text {
  margin: 0;
  color: rgba(224, 240, 255, 0.76);
  font-size: 14px;
  line-height: 1.8;
}

.empty-state {
  display: flex;
  min-height: 84px;
  align-items: center;
  justify-content: center;
  border: 1px dashed rgba(112, 183, 255, 0.18);
  border-radius: 8px;
  background: rgba(8, 18, 32, 0.6);
  color: rgba(216, 233, 255, 0.56);
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}

.empty-state.compact {
  min-height: 64px;
  padding: 10px 12px;
}

.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.evidence-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  border: 1px solid rgba(107, 175, 255, 0.12);
  border-radius: 8px;
  background: rgba(14, 24, 42, 0.9);
  padding: 10px;
}

.evidence-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(40, 61, 92, 0.9);
  color: rgba(227, 240, 255, 0.86);
  font-size: 12px;
  font-weight: 700;
}

.evidence-copy {
  min-width: 0;
}

.evidence-topline {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.evidence-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.evidence-provider {
  border: 1px solid rgba(141, 183, 255, 0.18);
  border-radius: 999px;
  background: rgba(19, 31, 52, 0.92);
  padding: 2px 8px;
  color: rgba(220, 235, 255, 0.78);
  font-size: 11px;
}

.evidence-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  color: rgba(214, 229, 249, 0.52);
  font-size: 12px;
}

.evidence-score {
  color: #8ae4ff;
}

.evidence-excerpt {
  margin: 8px 0 0;
  color: rgba(224, 240, 255, 0.72);
  font-size: 13px;
  line-height: 1.65;
}

.confidence-card {
  margin-top: 0;
}

.confidence-value {
  color: #7ee8ff;
  font-size: 18px;
  font-weight: 800;
}

.confidence-meter {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(48, 67, 94, 0.84);
}

.confidence-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #52bfff 0%, #6f7bff 48%, #7fe8cb 100%);
}

.confidence-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
  color: rgba(224, 240, 255, 0.56);
  font-size: 12px;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
}

.badge-good {
  border-color: rgba(111, 229, 180, 0.28);
  background: rgba(31, 79, 56, 0.35);
  color: #a5ffe1;
}

.badge-warm {
  border-color: rgba(255, 211, 117, 0.25);
  background: rgba(84, 60, 17, 0.38);
  color: #ffd98d;
}

.badge-cool {
  border-color: rgba(116, 167, 255, 0.25);
  background: rgba(22, 36, 69, 0.4);
  color: #bcdcff;
}

.pill-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.graph-pill {
  border: 1px solid rgba(99, 179, 255, 0.18);
  border-radius: 999px;
  background: rgba(14, 24, 42, 0.9);
  padding: 6px 10px;
  color: rgba(226, 240, 255, 0.82);
  font-size: 12px;
}

.path-list,
.source-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.path-row,
.source-row {
  border: 1px solid rgba(109, 177, 255, 0.12);
  border-radius: 8px;
  background: rgba(12, 20, 36, 0.9);
  padding: 10px 12px;
}

.path-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.path-key,
.source-title {
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.path-value {
  color: rgba(219, 235, 255, 0.72);
  font-size: 13px;
}

.source-subline {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 6px;
  color: rgba(219, 235, 255, 0.56);
  font-size: 12px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow: auto;
  padding: 16px;
  background: rgba(0, 5, 14, 0.72);
  backdrop-filter: blur(8px);
}

.agent-picker {
  display: flex;
  flex-direction: column;
  width: min(1180px, calc(100vw - 32px));
  max-height: calc(100dvh - 32px);
  overflow: hidden;
  box-sizing: border-box;
  border: 1px solid rgba(104, 175, 255, 0.26);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(8, 17, 33, 0.98), rgba(5, 11, 24, 0.99));
  padding: 16px;
  box-shadow: 0 28px 64px rgba(0, 0, 0, 0.5);
}

.picker-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.picker-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 20px;
  font-weight: 800;
}

.picker-subtitle {
  margin: 8px 0 0;
  color: rgba(222, 236, 255, 0.62);
  font-size: 13px;
  line-height: 1.6;
}

.picker-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.history-panel {
  margin-bottom: 14px;
  border: 1px solid rgba(95, 170, 255, 0.14);
  border-radius: 8px;
  background: rgba(12, 22, 40, 0.84);
  padding: 12px;
}

.history-hint {
  color: rgba(212, 229, 255, 0.46);
  font-size: 12px;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-room {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(113, 175, 255, 0.16);
  border-radius: 8px;
  background: rgba(10, 19, 34, 0.88);
  padding: 8px 10px;
  color: rgba(233, 244, 255, 0.88);
  text-align: left;
}

.history-room:hover {
  border-color: rgba(140, 220, 255, 0.34);
  background: rgba(22, 40, 70, 0.96);
}

.history-room.active {
  border-color: rgba(120, 198, 255, 0.42);
  background: rgba(26, 46, 82, 0.96);
}

.history-room-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.history-room-meta {
  flex-shrink: 0;
  border: 1px solid rgba(109, 179, 255, 0.16);
  border-radius: 999px;
  background: rgba(14, 28, 50, 0.88);
  padding: 2px 8px;
  color: rgba(214, 242, 255, 0.74);
  font-size: 11px;
}

.picker-summary-item {
  border: 1px solid rgba(95, 170, 255, 0.14);
  border-radius: 8px;
  background: rgba(12, 22, 40, 0.92);
  padding: 10px 12px;
}

.summary-key {
  display: block;
  color: rgba(212, 229, 255, 0.52);
  font-size: 12px;
}

.summary-val {
  display: block;
  margin-top: 4px;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.picker-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  overflow: auto;
  padding-right: 4px;
}

.picker-card {
  position: relative;
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 10px;
  border: 1px solid rgba(98, 176, 255, 0.18);
  border-radius: 8px;
  background: rgba(11, 20, 35, 0.96);
  padding: 10px;
  text-align: left;
}

.picker-card.selected {
  border-color: rgba(117, 221, 255, 0.42);
  background: rgba(26, 41, 67, 0.96);
  box-shadow: inset 0 0 0 1px rgba(128, 225, 255, 0.08);
}

.picker-card:hover {
  background: rgba(21, 34, 56, 0.98);
}

.picker-avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  border: 1px solid rgba(159, 209, 255, 0.14);
}

.picker-copy {
  min-width: 0;
}

.picker-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.picker-name {
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.picker-tag {
  border: 1px solid rgba(151, 110, 255, 0.34);
  border-radius: 999px;
  background: rgba(89, 63, 163, 0.24);
  padding: 2px 8px;
  color: #dccfff;
  font-size: 11px;
}

.picker-desc {
  margin: 0;
  color: rgba(220, 233, 255, 0.66);
  font-size: 13px;
  line-height: 1.55;
}

.picker-flag {
  position: absolute;
  right: 10px;
  bottom: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border-radius: 999px;
  background: rgba(18, 68, 54, 0.9);
  padding: 4px 8px;
  color: #9af8ce;
  font-size: 11px;
  opacity: 0;
  transform: translateY(2px);
}

.picker-flag.visible {
  opacity: 1;
}

.picker-foot {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.picker-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: rgba(219, 233, 255, 0.56);
  font-size: 12px;
}

.picker-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.secondary-btn,
.primary-btn {
  height: 38px;
  padding: 0 14px;
  font-size: 13px;
}

.secondary-btn {
  border: 1px solid rgba(108, 176, 255, 0.18);
  background: rgba(11, 20, 35, 0.92);
  color: rgba(233, 244, 255, 0.88);
}

.primary-btn {
  border: 1px solid rgba(120, 198, 255, 0.32);
  background: linear-gradient(180deg, #6f84ff 0%, #4f69f5 100%);
  color: #fff;
}

.primary-btn:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.icon-xs {
  width: 12px;
  height: 12px;
}

.icon-sm {
  width: 14px;
  height: 14px;
}

.icon-md {
  width: 16px;
  height: 16px;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.scrollbar-track-transparent {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.32) transparent;
}

.scrollbar-track-transparent::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.scrollbar-track-transparent::-webkit-scrollbar-track {
  background: transparent;
}

.scrollbar-track-transparent::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background-color: rgba(255, 255, 255, 0.32);
}

@media (max-width: 1360px) {
  .workspace {
    grid-template-columns: minmax(300px, 0.86fr) minmax(0, 1.32fr) minmax(300px, 0.84fr);
  }

  .hero-copy h1 {
    font-size: 34px;
  }
}

@media (max-width: 1120px) {
  .workspace {
    grid-template-columns: 1fr;
    height: auto;
  }

  .agent-hero {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .hero-copy,
  .hero-actions,
  .brand-mark {
    justify-self: start;
  }

  .hero-copy {
    text-align: left;
  }

  .picker-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .agent-console {
    overflow: auto;
  }

  .agent-hero {
    padding-top: 6px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .hero-actions {
    flex-wrap: wrap;
  }

  .picker-grid {
    grid-template-columns: 1fr;
  }

  .picker-foot,
  .composer-actions,
  .confidence-foot {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
