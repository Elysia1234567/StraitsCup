import { ref, shallowRef, onUnmounted } from 'vue';
import * as chatApi from '@/api/chatApi.js';
import { toAgentViewModel } from '@/utils/agentAssets.js';

function mapHistoryMessage(m) {
  const base = {
    id: m.id,
    messageId: m.id,
    streamId: m.streamId,
    senderType: m.senderType,
    senderName: m.senderName,
    senderAvatar: toAgentViewModel({ agentCode: m.senderId, avatar: m.senderAvatar }).avatar,
    content: m.content ?? '',
    imageUrl: m.imageUrl,
    messageType: m.messageType,
    streaming: false,
  };
  if (m.senderType === 'USER') {
    return { ...base, role: 'user' };
  }
  if (m.senderType === 'AGENT') {
    return { ...base, role: 'assistant' };
  }
  return { ...base, role: 'system' };
}

export function useMultiAgentChat() {
  const rooms = ref([]);
  const currentRoomId = ref(null);
  /** 当前房间内参与聊天的智能体成员（用于顶栏头像） */
  const roomChatAgents = ref([]);
  const messages = ref([]);
  const wsConnected = ref(false);
  const wsError = ref(null);
  const loadingRooms = ref(false);
  const loadingMessages = ref(false);
  const ws = shallowRef(null);
  /** @type {Map<string, number>} streamId -> index in messages */
  const streamIndex = new Map();

  function disconnectWs() {
    if (ws.value) {
      ws.value.onopen = null;
      ws.value.onmessage = null;
      ws.value.onclose = null;
      ws.value.onerror = null;
      ws.value.close();
      ws.value = null;
    }
    wsConnected.value = false;
    streamIndex.clear();
  }

  async function loadRooms() {
    loadingRooms.value = true;
    wsError.value = null;
    try {
      rooms.value = await chatApi.fetchRooms();
    } catch (e) {
      wsError.value = e instanceof Error ? e.message : String(e);
      rooms.value = [];
    } finally {
      loadingRooms.value = false;
    }
  }

  function connectWs(roomId) {
    disconnectWs();
    const url = chatApi.buildChatWebSocketUrl(roomId);
    const socket = new WebSocket(url);

    socket.onopen = () => {
      wsConnected.value = true;
      wsError.value = null;
    };

    socket.onmessage = (ev) => {
      try {
        handleWsMessage(JSON.parse(ev.data));
      } catch (e) {
        wsError.value = e instanceof Error ? e.message : String(e);
      }
    };

    socket.onclose = () => {
      wsConnected.value = false;
    };

    socket.onerror = () => {
      wsError.value = 'WebSocket 连接异常';
    };

    ws.value = socket;
  }

  function removePendingUserByContent(content) {
    const i = messages.value.findIndex((m) => m.role === 'user' && m.pending && m.content === content);
    if (i !== -1) {
      messages.value.splice(i, 1);
    }
  }

  function handleWsMessage(msg) {
    const type = msg.type;

    switch (type) {
      case 'CHAT': {
        if (msg.senderType === 'USER') {
          removePendingUserByContent(msg.content || '');
          messages.value.push({
            role: 'user',
            id: msg.messageId,
            messageId: msg.messageId,
            content: msg.content || '',
            imageUrl: msg.imageUrl,
            messageType: msg.messageType || 'TEXT',
          });
        }
        break;
      }
      case 'AGENT_START': {
        messages.value.push({
          role: 'assistant',
          streamId: msg.streamId,
          senderId: msg.senderId,
          senderName: msg.senderName,
          senderAvatar: toAgentViewModel({ agentCode: msg.senderId, avatar: msg.senderAvatar }).avatar,
          content: '',
          streaming: true,
          messageType: 'TEXT',
        });
        streamIndex.set(msg.streamId, messages.value.length - 1);
        break;
      }
      case 'AGENT_CHUNK': {
        const idx = streamIndex.get(msg.streamId);
        if (idx == null) break;
        const row = messages.value[idx];
        if (row) {
          row.content = (row.content || '') + (msg.content || '');
        }
        break;
      }
      case 'AGENT_END': {
        const idx = streamIndex.get(msg.streamId);
        if (idx == null) break;
        const row = messages.value[idx];
        if (row) {
          row.content = msg.content != null && msg.content !== '' ? msg.content : row.content;
          row.streaming = false;
          row.messageId = msg.messageId;
          row.id = msg.messageId;
        }
        streamIndex.delete(msg.streamId);
        break;
      }
      case 'IMAGE': {
        if (msg.senderType === 'AGENT') {
          messages.value.push({
            role: 'assistant',
            id: msg.messageId,
            messageId: msg.messageId,
            senderId: msg.senderId,
            senderName: msg.senderName,
            senderAvatar: toAgentViewModel({ agentCode: msg.senderId, avatar: msg.senderAvatar }).avatar,
            content: msg.content || '',
            imageUrl: msg.imageUrl,
            messageType: 'IMAGE',
            streaming: false,
          });
        }
        break;
      }
      case 'SYSTEM': {
        messages.value.push({
          role: 'system',
          content: msg.content || '',
          onlineCount: msg.onlineCount,
        });
        break;
      }
      case 'ERROR': {
        messages.value.push({
          role: 'system',
          content: msg.senderType === 'AGENT' ? `${msg.senderName || 'Agent'}：${msg.content || '出错'}` : msg.content || '错误',
        });
        break;
      }
      default:
        break;
    }
  }

  async function loadRoomChatAgents(roomId) {
    roomChatAgents.value = [];
    try {
      const [members, agents] = await Promise.all([
        chatApi.fetchRoomAgents(roomId),
        chatApi.fetchAgents(),
      ]);
      const agentMap = new Map((agents || []).map((agent) => [agent.id, agent]));
      roomChatAgents.value = (members || [])
        .filter((m) => m.memberType === 'AGENT' && m.agentId != null)
        .map((m) => ({
          id: m.id,
          agentId: m.agentId,
          name: m.displayName || '智能体',
          avatar: toAgentViewModel(agentMap.get(m.agentId) || { avatar: m.avatar }).avatar,
          agentCode: agentMap.get(m.agentId)?.agentCode,
          appearanceUrl: toAgentViewModel(agentMap.get(m.agentId) || { avatar: m.avatar }).appearanceUrl,
          sourceImageUrl: toAgentViewModel(agentMap.get(m.agentId) || {}).sourceImageUrl,
        }));
    } catch {
      roomChatAgents.value = [];
    }
  }

  async function refreshRoomChatAgents() {
    if (!currentRoomId.value) return;
    await loadRoomChatAgents(currentRoomId.value);
  }

  async function addRoomAgentToCurrentRoom(agentCode) {
    if (!currentRoomId.value) return;
    await chatApi.addRoomAgent(currentRoomId.value, agentCode);
    await refreshRoomChatAgents();
  }

  async function removeRoomAgentFromCurrentRoom(memberId) {
    if (!currentRoomId.value) return;
    await chatApi.removeRoomAgent(currentRoomId.value, memberId);
    await refreshRoomChatAgents();
  }

  async function openRoom(roomId) {
    if (!roomId) return;
    loadingMessages.value = true;
    wsError.value = null;
    currentRoomId.value = roomId;
    try {
      const list = await chatApi.fetchRecentMessages(roomId, 100);
      const chronological = Array.isArray(list) ? [...list].reverse() : [];
      messages.value = chronological.map(mapHistoryMessage);
      await loadRoomChatAgents(roomId);
    } catch (e) {
      wsError.value = e instanceof Error ? e.message : String(e);
      messages.value = [];
      roomChatAgents.value = [];
    } finally {
      loadingMessages.value = false;
    }
    connectWs(roomId);
  }

  async function deleteChatRoom(roomId) {
    await chatApi.deleteRoom(roomId);
    await loadRooms();
    if (currentRoomId.value === roomId) {
      disconnectWs();
      currentRoomId.value = null;
      messages.value = [];
      roomChatAgents.value = [];
      if (rooms.value.length > 0) {
        await openRoom(rooms.value[0].id);
      }
    }
  }

  async function createNewRoom({ name, agentCodes } = {}) {
    const room = await chatApi.createRoom({
      name: name || `对话 ${new Date().toLocaleString('zh-CN', { hour12: false })}`,
      agentCodes: agentCodes?.length ? agentCodes : undefined,
    });
    await loadRooms();
    await openRoom(room.id);
    return room;
  }

  function sendUserMessage(text, { searchEnabled = false } = {}) {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN || !currentRoomId.value) {
      wsError.value = '未连接到聊天室';
      return;
    }
    const trimmed = text.trim();
    if (!trimmed) return;

    messages.value.push({
      role: 'user',
      content: trimmed,
      pending: true,
    });

    const payload = {
      type: 'CHAT',
      senderType: 'USER',
      content: trimmed,
      metadata: {
        searchEnabled,
      },
    };
    ws.value.send(JSON.stringify(payload));
  }

  onUnmounted(() => {
    disconnectWs();
  });

  return {
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
    disconnectWs,
  };
}
