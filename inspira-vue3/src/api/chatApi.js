import { apiFetch } from './request.js';

export function buildChatWebSocketUrl(roomId) {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws/chat?roomId=${encodeURIComponent(roomId)}`;
}

export function buildVoiceWebSocketUrl(dialect = 'mandarin') {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const params = new URLSearchParams({ dialect });
  return `${proto}//${window.location.host}/ws/voice?${params.toString()}`;
}

export function fetchRooms() {
  return apiFetch('/api/chat-rooms');
}

/**
 * @param {{ name: string, themeId?: number, agentCodes?: string[] }} payload
 */
export function createRoom(payload) {
  return apiFetch('/api/chat-rooms', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function fetchRecentMessages(roomId, limit = 80) {
  return apiFetch(`/api/chat-rooms/${roomId}/messages/recent?limit=${limit}`);
}

export function fetchAgents() {
  return apiFetch('/api/agents');
}

export function fetchRoomAgents(roomId) {
  return apiFetch(`/api/chat-rooms/${roomId}/agents`);
}

export function addRoomAgent(roomId, agentCode) {
  return apiFetch(`/api/chat-rooms/${roomId}/agents`, {
    method: 'POST',
    body: JSON.stringify({ agentCode }),
  });
}

export function removeRoomAgent(roomId, memberId) {
  return apiFetch(`/api/chat-rooms/${roomId}/agents/${memberId}`, { method: 'DELETE' });
}

export function deleteRoom(roomId) {
  return apiFetch(`/api/chat-rooms/${roomId}`, { method: 'DELETE' });
}

export function generateAgentImage(payload) {
  return apiFetch('/api/aigc/image', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function chatOnce(message) {
  const params = new URLSearchParams({ message });
  return fetch(`/api/aigc/chat?${params.toString()}`).then(async (res) => {
    const text = await res.text();
    if (!res.ok) {
      throw new Error(text || `HTTP ${res.status}`);
    }
    return text;
  });
}

export function fetchMcpTools() {
  return apiFetch('/api/mcp/tools');
}

export function callMcpTool(name, args) {
  return apiFetch(`/api/mcp/tools/${encodeURIComponent(name)}/call`, {
    method: 'POST',
    body: JSON.stringify({ arguments: args || {} }),
  });
}
