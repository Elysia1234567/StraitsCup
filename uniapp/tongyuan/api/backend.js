/**
 * OmniSource 后端全量 HTTP API（与 StraitsCup/BackEnd 对齐）
 * WebSocket 见 utils/chatSocket.js
 */
import { requestData, requestResult, getAigcChatPlain, getAigcStreamUrl, uploadImage } from '@/utils/http.js'

export { getApiBase, getWsChatUrl, getWsVoiceUrl, getAigcStreamUrl, uploadImage, getAigcChatPlain } from '@/utils/http.js'
export { ApiError } from '@/utils/http.js'

// —— AIGC ——
export function postAigcChat(message) {
  return requestData({
    method: 'POST',
    path: '/api/aigc/chat',
    data: { message },
  }).then((d) => (d == null ? '' : String(d)))
}

export function postAigcMultimodal(imageUrl, question) {
  return requestData({
    method: 'POST',
    path: '/api/aigc/multimodal',
    data: { imageUrl, question: question || '请描述这张图片' },
  }).then((d) => (d == null ? '' : String(d)))
}

export function postAigcImage(body) {
  return requestData({
    method: 'POST',
    path: '/api/aigc/image',
    data: body || {},
  })
}

export function getAigcImageTask(taskId) {
  return requestData({
    method: 'GET',
    path: `/api/aigc/image/tasks/${encodeURIComponent(taskId)}`,
  })
}

// —— Agent ——
export function getAgents() {
  return requestData({ method: 'GET', path: '/api/agents' })
}

export function getAgentByCode(code) {
  return requestData({ method: 'GET', path: `/api/agents/${encodeURIComponent(code)}` })
}

// —— 多智能体 REST 问答 ——
export function postMultiAgentChat(body) {
  return requestData({
    method: 'POST',
    path: '/api/chat',
    data: body,
  })
}

export function getMultiAgentSession(sessionId) {
  return requestData({
    method: 'GET',
    path: `/api/chat/${encodeURIComponent(sessionId)}`,
  })
}

// —— 聊天室 ——
export function getChatRooms() {
  return requestData({ method: 'GET', path: '/api/chat-rooms' })
}

export function postChatRoom(body) {
  return requestData({
    method: 'POST',
    path: '/api/chat-rooms',
    data: body || {},
  })
}

export function getChatRoom(roomId) {
  return requestData({
    method: 'GET',
    path: `/api/chat-rooms/${encodeURIComponent(roomId)}`,
  })
}

export function deleteChatRoom(roomId) {
  return requestData({
    method: 'DELETE',
    path: `/api/chat-rooms/${encodeURIComponent(roomId)}`,
  })
}

export function getChatRoomAgents(roomId) {
  return requestData({
    method: 'GET',
    path: `/api/chat-rooms/${encodeURIComponent(roomId)}/agents`,
  })
}

export function postChatRoomAgent(roomId, agentCode) {
  return requestData({
    method: 'POST',
    path: `/api/chat-rooms/${encodeURIComponent(roomId)}/agents`,
    data: { agentCode },
  })
}

export function putChatRoomAgent(roomId, memberId, agentCode) {
  return requestData({
    method: 'PUT',
    path: `/api/chat-rooms/${encodeURIComponent(roomId)}/agents/${encodeURIComponent(memberId)}`,
    data: { agentCode },
  })
}

export function deleteChatRoomAgent(roomId, memberId) {
  return requestData({
    method: 'DELETE',
    path: `/api/chat-rooms/${encodeURIComponent(roomId)}/agents/${encodeURIComponent(memberId)}`,
  })
}

export function getChatRoomMessages(roomId, page = 1, size = 20) {
  return requestData({
    method: 'GET',
    path: `/api/chat-rooms/${encodeURIComponent(roomId)}/messages`,
    query: { page, size },
  })
}

export function getChatRoomMessagesRecent(roomId, limit = 50) {
  return requestData({
    method: 'GET',
    path: `/api/chat-rooms/${encodeURIComponent(roomId)}/messages/recent`,
    query: { limit },
  })
}

// —— RAG ——
export function postRagReload() {
  return requestData({ method: 'POST', path: '/api/rag/reload' })
}

export function getRagRetrieve(question, topK = 3) {
  return requestData({
    method: 'GET',
    path: '/api/rag/retrieve',
    query: { question, topK },
  })
}

export function getRagPrompt(question, topK = 3) {
  return requestData({
    method: 'GET',
    path: '/api/rag/prompt',
    query: { question, topK },
  })
}

// —— 系统画像 ——
export function getSystemProfile() {
  return requestData({ method: 'GET', path: '/api/system-profile' })
}

// —— MCP ——
export function getMcpTools() {
  return requestData({ method: 'GET', path: '/api/mcp/tools' })
}

export function postMcpToolCall(name, argumentsObj) {
  return requestData({
    method: 'POST',
    path: `/api/mcp/tools/${encodeURIComponent(name)}/call`,
    data: { arguments: argumentsObj || {} },
  })
}

/** 原始 Result（如仅需判断 code） */
export function rawRequest(opts) {
  return requestResult(opts)
}
