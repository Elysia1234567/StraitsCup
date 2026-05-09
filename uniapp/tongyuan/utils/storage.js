/** 与后端对接相关的本地存储键（默认用户 userId=1，与后端文档一致） */
export const STORAGE = {
  ACTIVE_ROOM_ID: 'omnisource_activeRoomId',
  MULTI_AGENT_SESSION_ID: 'omnisource_multiAgentSessionId',
  PENDING_AGENT_CODES: 'omnisource_pendingAgentCodes',
}

export function getActiveRoomId() {
  const v = uni.getStorageSync(STORAGE.ACTIVE_ROOM_ID)
  if (v === '' || v == null) return null
  const s = String(v)
  if (s.startsWith('mock-')) return s
  const n = Number(s)
  return Number.isFinite(n) && n > 0 ? n : null
}

export function setActiveRoomId(roomId) {
  if (roomId == null) {
    uni.removeStorageSync(STORAGE.ACTIVE_ROOM_ID)
    return
  }
  uni.setStorageSync(STORAGE.ACTIVE_ROOM_ID, String(roomId))
}

export function getPendingAgentCodes() {
  try {
    const raw = uni.getStorageSync(STORAGE.PENDING_AGENT_CODES)
    if (!raw) return null
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) && arr.length ? arr.map(String) : null
  } catch {
    return null
  }
}

export function setPendingAgentCodes(codes) {
  if (!codes || !codes.length) {
    uni.removeStorageSync(STORAGE.PENDING_AGENT_CODES)
    return
  }
  uni.setStorageSync(STORAGE.PENDING_AGENT_CODES, JSON.stringify(codes))
}

export function getMultiAgentSessionId() {
  const s = uni.getStorageSync(STORAGE.MULTI_AGENT_SESSION_ID)
  return s ? String(s) : null
}

export function setMultiAgentSessionId(id) {
  if (!id) uni.removeStorageSync(STORAGE.MULTI_AGENT_SESSION_ID)
  else uni.setStorageSync(STORAGE.MULTI_AGENT_SESSION_ID, String(id))
}
