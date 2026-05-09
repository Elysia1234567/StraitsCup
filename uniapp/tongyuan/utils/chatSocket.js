/**
 * 聊天室 WebSocket（/ws/chat?roomId=），与后端 ChatWebSocketHandler 对齐。
 * 小程序全局仅一条 socket，切换房间请先 disconnect。
 */
import { getWsChatUrl } from '@/utils/http.js'

let messageHandler = null
let openHandler = null
let closeHandler = null
let errorHandler = null
let hooked = false

function ensureHooks() {
  if (hooked) return
  hooked = true
  uni.onSocketMessage((res) => {
    if (messageHandler) messageHandler(res)
  })
  uni.onSocketOpen(() => {
    if (openHandler) openHandler()
  })
  uni.onSocketClose((res) => {
    if (closeHandler) closeHandler(res)
  })
  uni.onSocketError((res) => {
    if (errorHandler) errorHandler(res)
  })
}

/**
 * @param {number|string} roomId
 * @param {{ onMessage?: (res:{data:string})=>void, onOpen?: ()=>void, onClose?: (res:any)=>void, onError?: (res:any)=>void }} handlers
 */
export function connectRoomChat(roomId, handlers = {}) {
  ensureHooks()
  messageHandler = handlers.onMessage || null
  openHandler = handlers.onOpen || null
  closeHandler = handlers.onClose || null
  errorHandler = handlers.onError || null
  const url = getWsChatUrl(roomId)
  uni.closeSocket({
    complete() {
      uni.connectSocket({
        url,
        fail(err) {
          if (errorHandler) errorHandler(err)
        },
      })
    },
  })
}

/** 发送用户文本（metadata 可选 searchEnabled / ragEnabled） */
export function sendRoomUserChat(content, metadata) {
  const payload = {
    type: 'CHAT',
    content: content || '',
    metadata: metadata || {},
  }
  uni.sendSocketMessage({
    data: JSON.stringify(payload),
    fail(err) {
      console.warn('[ws] send fail', err)
    },
  })
}

export function disconnectRoomChat() {
  messageHandler = null
  openHandler = null
  closeHandler = null
  errorHandler = null
  try {
    uni.closeSocket({})
  } catch {
    /* noop */
  }
}
