import { apiFetch } from './request.js';

export function buildChatWebSocketUrl(roomId) {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws/chat?roomId=${encodeURIComponent(roomId)}`;
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

export function deleteRoom(roomId) {
  return apiFetch(`/api/chat-rooms/${roomId}`, { method: 'DELETE' });
}

export function generateAgentImage(payload) {
  return apiFetch('/api/aigc/image', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
