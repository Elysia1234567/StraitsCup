<template>
  <view class="chat-container wot-theme-dark cyber-wot">
    <view v-if="roomId" class="room-bar">
      <text class="room-text">{{ roomBarTitle }}</text>
      <wd-button size="small" plain hairline type="primary" custom-class="room-leave" @click="leaveRoom">
        离开房间
      </wd-button>
    </view>
    <view v-else-if="pendingCodesLabel" class="room-bar">
      <text class="room-text">多智能体：{{ pendingCodesLabel }}</text>
      <wd-button size="small" plain hairline @click="clearPendingAgents">清除</wd-button>
    </view>

    <scroll-view
      class="message-list"
      :class="{ 'with-bar': roomId || pendingCodesLabel }"
      scroll-y
      :scroll-into-view="scrollIntoView"
      :scroll-with-animation="true"
    >
      <view
        v-for="(item, index) in messageList"
        :id="'msg-' + index"
        :key="index"
        class="message-item"
        :class="item.type === 'ai' ? 'ai-message' : 'user-message'"
      >
        <template v-if="item.type === 'ai'">
          <view class="avatar ai-avatar">
            <wd-icon name="chat" size="36rpx" color="#061018" />
          </view>
          <view class="message-bubble ai-bubble">
            <text v-if="item.senderName" class="sender-name">{{ item.senderName }}</text>
            <text class="bubble-text ai-text">{{ item.content }}</text>
            <image v-if="item.imageUrl" class="bubble-image" :src="item.imageUrl" mode="widthFix" />
          </view>
        </template>
        <template v-else>
          <view class="message-bubble user-bubble">
            <text class="bubble-text user-text">{{ item.content }}</text>
            <image v-if="item.imageUrl" class="bubble-image" :src="item.imageUrl" mode="widthFix" />
          </view>
          <view class="avatar user-avatar">
            <wd-icon name="user" size="36rpx" color="#d0d0d0" />
          </view>
        </template>
      </view>
      <view id="msg-end" class="scroll-anchor" />
    </scroll-view>

    <view class="input-bar">
      <wd-input
        v-model="inputText"
        custom-class="cyber-chat-input"
        custom-input-class="cyber-chat-input-inner"
        no-border
        placeholder="有什么想聊的，尽管说~"
        placeholder-style="color: rgba(176,176,176,0.75);"
        confirm-type="send"
        clearable
        prefix-icon="chat"
        @confirm="sendMessage"
      />
      <wd-button
        type="primary"
        plain
        hairline
        round
        size="small"
        icon="arrow-right"
        custom-class="cyber-send-btn"
        :loading="sending"
        :disabled="!inputText.trim() || sending"
        @click="sendMessage"
      >
        发送
      </wd-button>
    </view>
  </view>
</template>

<script>
import { postAigcChat, postMultiAgentChat, getChatRoomMessagesRecent } from '@/api/backend.js'
import { getMockHistoryMessages, getMockChatReply, isMockRoomId } from '@/utils/mockChat.js'
import { connectRoomChat, disconnectRoomChat, sendRoomUserChat } from '@/utils/chatSocket.js'
import {
  getActiveRoomId,
  setActiveRoomId,
  getPendingAgentCodes,
  setPendingAgentCodes,
  getMultiAgentSessionId,
  setMultiAgentSessionId,
} from '@/utils/storage.js'

export default {
  data() {
    return {
      inputText: '',
      sending: false,
      roomId: null,
      socketReady: false,
      pendingCodes: null,
      /** 最近一次本地发送的原文，用于去重 WS 回显的 USER CHAT */
      lastLocalUserEcho: '',
      messageList: [],
      scrollIntoView: '',
    }
  },
  computed: {
    pendingCodesLabel() {
      const c = this.pendingCodes
      if (!c || !c.length) return ''
      return c.slice(0, 3).join('、') + (c.length > 3 ? '…' : '')
    },
    roomBarTitle() {
      const id = this.roomId
      if (!id) return ''
      if (isMockRoomId(id)) {
        const titles = {
          'mock-heritage-craft': '离线演示 · 福州软木画',
          'mock-travel-food': '离线演示 · 泉州古城',
          'mock-multi-agent': '离线演示 · 器灵协作脚本',
        }
        return titles[id] || '离线演示聊天室'
      }
      return `房间 #${id}`
    },
  },
  watch: {
    messageList: {
      handler() {
        this.$nextTick(() => {
          this.scrollIntoView = 'msg-end'
        })
      },
      deep: true,
    },
  },
  onShow() {
    const rid = getActiveRoomId()
    const pending = getPendingAgentCodes()
    const prevRoom = this.roomId
    this.roomId = rid
    this.pendingCodes = pending

    if (this.roomId) {
      if (prevRoom !== this.roomId) {
        this.messageList = []
        this.loadRoomHistory()
      }
      if (!isMockRoomId(this.roomId)) {
        this.setupSocket()
      }
      return
    }

    this.teardownSocket()
    if (!this.messageList.length) {
      this.messageList.push({ type: 'ai', content: this.welcomeLine() })
    }
  },
  onHide() {
    this.teardownSocket()
  },
  methods: {
    welcomeLine() {
      if (this.roomId && isMockRoomId(this.roomId)) {
        return '【离线演示】未连接后端，下方为模拟历史；发送消息仅作本地示意，不会上传服务器。'
      }
      if (this.roomId) {
        return `已进入聊天室（房间 ID：${this.roomId}），消息经 WebSocket 与多器灵协作。`
      }
      if (this.pendingCodes && this.pendingCodes.length) {
        return `已选择器灵：将使用「多智能体 REST」回答（会话 ${getMultiAgentSessionId() || '新'}）。`
      }
      return '自由模式：直连 POST /api/aigc/chat 单轮问答。（首页可进聊天室并体验 WebSocket）'
    },
    mapHistoryRow(m) {
      const st = String(m.senderType || '').toUpperCase()
      const type = st === 'USER' ? 'user' : 'ai'
      return {
        type,
        content: m.content || '',
        imageUrl: m.imageUrl || '',
        senderName: st === 'AGENT' ? m.senderName || '' : '',
        streaming: false,
      }
    },
    async loadRoomHistory() {
      if (!this.roomId) return
      if (isMockRoomId(this.roomId)) {
        const mapped = getMockHistoryMessages(this.roomId)
        this.messageList = [{ type: 'ai', content: this.welcomeLine() }, ...mapped]
        return
      }
      try {
        const list = await getChatRoomMessagesRecent(this.roomId, 80)
        if (!Array.isArray(list) || !list.length) return
        const mapped = list.map((m) => this.mapHistoryRow(m))
        this.messageList = [{ type: 'ai', content: this.welcomeLine() }, ...mapped]
      } catch (e) {
        console.warn('load history', e)
        uni.showToast({ title: '加载历史失败', icon: 'none' })
      }
    },
    setupSocket() {
      const rid = this.roomId
      if (!rid) return
      connectRoomChat(rid, {
        onOpen: () => {
          this.socketReady = true
        },
        onClose: () => {
          this.socketReady = false
        },
        onError: () => {
          this.socketReady = false
        },
        onMessage: (res) => this.onSocketMessage(res.data),
      })
    },
    teardownSocket() {
      disconnectRoomChat()
      this.socketReady = false
    },
    onSocketMessage(raw) {
      let m
      try {
        m = typeof raw === 'string' ? JSON.parse(raw) : JSON.parse(JSON.stringify(raw))
      } catch {
        return
      }
      const t = m.type
      if (!t) return

      if (t === 'CHAT') {
        const st = String(m.senderType || '').toUpperCase()
        if (st === 'USER' && m.content === this.lastLocalUserEcho) {
          return
        }
        const row = {
          type: st === 'USER' ? 'user' : 'ai',
          content: m.content || '',
          imageUrl: m.imageUrl || '',
          senderName: st === 'AGENT' ? m.senderName || '' : '',
        }
        this.messageList.push(row)
        return
      }

      if (t === 'AGENT_START') {
        this.messageList.push({
          type: 'ai',
          content: '',
          senderName: m.senderName || '',
          streamId: m.streamId,
          streaming: true,
        })
        return
      }

      if (t === 'AGENT_CHUNK' && m.streamId) {
        for (let i = this.messageList.length - 1; i >= 0; i--) {
          const it = this.messageList[i]
          if (it.streamId === m.streamId && it.streaming) {
            it.content = (it.content || '') + (m.content || '')
            break
          }
        }
        return
      }

      if (t === 'AGENT_END' && m.streamId) {
        for (let i = this.messageList.length - 1; i >= 0; i--) {
          const it = this.messageList[i]
          if (it.streamId === m.streamId && it.streaming) {
            if (m.content != null && String(m.content)) {
              it.content = String(m.content)
            }
            it.streaming = false
            break
          }
        }
        return
      }

      if (t === 'IMAGE') {
        this.messageList.push({
          type: 'ai',
          content: m.content || '',
          imageUrl: m.imageUrl || '',
          senderName: m.senderName || '',
        })
        return
      }

      if (t === 'SYSTEM' || t === 'TYPING' || t === 'PROGRESS') {
        if (t === 'SYSTEM') {
          this.messageList.push({ type: 'ai', content: `[系统] ${m.content || ''}` })
        }
        return
      }

      if (t === 'ERROR') {
        this.messageList.push({
          type: 'ai',
          content: `[错误] ${m.content || m.senderName || '未知错误'}`,
        })
      }
    },
    leaveRoom() {
      setActiveRoomId(null)
      this.roomId = null
      this.teardownSocket()
      this.messageList = [{ type: 'ai', content: this.welcomeLine() }]
    },
    clearPendingAgents() {
      setPendingAgentCodes(null)
      this.pendingCodes = null
      setMultiAgentSessionId(null)
      this.messageList = [{ type: 'ai', content: this.welcomeLine() }]
    },
    async sendMessage() {
      const t = this.inputText.trim()
      if (!t || this.sending) return

      this.messageList.push({ type: 'user', content: t })
      this.inputText = ''
      this.sending = true

      try {
        if (this.roomId && isMockRoomId(this.roomId)) {
          this.messageList.push({
            type: 'ai',
            content: getMockChatReply(this.roomId, t),
            senderName: '同源演示',
          })
          this.sending = false
          return
        }

        if (this.roomId) {
          this.lastLocalUserEcho = t
          setTimeout(() => {
            this.lastLocalUserEcho = ''
          }, 800)
          if (!this.socketReady) {
            this.setupSocket()
            await new Promise((r) => setTimeout(r, 400))
          }
          sendRoomUserChat(t, { searchEnabled: false, ragEnabled: true })
          this.sending = false
          return
        }

        const codes = getPendingAgentCodes()
        if (codes && codes.length) {
          const sessionId = getMultiAgentSessionId()
          const res = await postMultiAgentChat({
            sessionId: sessionId || undefined,
            query: t,
            topK: 3,
            searchEnabled: false,
            agentCodes: codes,
          })
          if (res && res.sessionId) {
            setMultiAgentSessionId(res.sessionId)
          }
          const answer =
            res && res.finalAnswer && String(res.finalAnswer).trim()
              ? String(res.finalAnswer).trim()
              : formatMultiAgentReplies(res)
          this.messageList.push({ type: 'ai', content: answer || '（无聚合回答）' })
          this.sending = false
          return
        }

        const reply = await postAigcChat(t)
        const text = reply && String(reply).trim() ? reply : '（模型返回为空）'
        this.messageList.push({ type: 'ai', content: text })
      } catch (e) {
        const msg = e && e.message ? e.message : '请求失败'
        this.messageList.push({
          type: 'ai',
          content: `请求失败：${msg}`,
        })
        uni.showToast({ title: '发送失败', icon: 'none', duration: 2500 })
      } finally {
        this.sending = false
      }
    },
  },
}

function formatMultiAgentReplies(res) {
  if (!res || !Array.isArray(res.agentReplies)) return ''
  return res.agentReplies
    .map((r) => `【${r.title || r.agentCode || 'Agent'}】${r.content || ''}`)
    .join('\n\n')
}
</script>

<style lang="scss" scoped>
@import '../../styles/cyber.scss';

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-sizing: border-box;
  @include cyber-bg-grid;
}

.room-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16rpx 20rpx 0;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: $cyber-bg-panel-elevated;
  @include glow-border-purple;
  box-shadow: 0 8rpx 36rpx rgba(22, 93, 255, 0.1);
}

.room-text {
  font-size: 24rpx;
  color: $cyber-text-muted;
  flex: 1;
  min-width: 0;
  padding-right: 16rpx;
}

.message-list {
  flex: 1;
  height: 0;
  padding: 20rpx;
  box-sizing: border-box;
}

.message-list.with-bar {
  padding-top: 8rpx;
}

.scroll-anchor {
  height: 1px;
  width: 100%;
}

.message-item {
  display: flex;
  margin-bottom: 30rpx;
  align-items: flex-start;
}

.ai-message {
  justify-content: flex-start;
}

.user-message {
  justify-content: flex-end;
}

.avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-avatar {
  background: linear-gradient(145deg, $cyber-neon-purple-soft, #0e4bcc);
  border: 1rpx solid rgba(22, 93, 255, 0.55);
  box-shadow: 0 0 22rpx rgba(22, 93, 255, 0.35);
}

.user-avatar {
  background: linear-gradient(160deg, #1e293b, #0f172a);
  border: 1rpx solid rgba(22, 93, 255, 0.35);
  box-shadow: 0 0 18rpx rgba(0, 0, 0, 0.45);
}

.message-bubble {
  max-width: 60%;
  padding: 22rpx 26rpx;
  border-radius: $cyber-radius-lg;
  font-size: 28rpx;
  line-height: 1.5;
  word-break: break-word;
}

.ai-bubble {
  margin-left: 16rpx;
  background: rgba(18, 18, 18, 0.94);
  @include glow-border-purple;
}

.user-bubble {
  margin-right: 16rpx;
  background: rgba(22, 28, 40, 0.88);
  @include glow-border-magenta;
}

.sender-name {
  display: block;
  font-size: 22rpx;
  color: $cyber-neon-purple-soft;
  margin-bottom: 8rpx;
}

.bubble-text {
  display: block;
}

.bubble-image {
  margin-top: 12rpx;
  max-width: 100%;
  border-radius: $cyber-radius-md;
  display: block;
}

.ai-text {
  @include neon-text-purple;
}

.user-text {
  color: #ffffff;
  text-shadow:
    0 0 8rpx rgba(22, 93, 255, 0.35),
    0 0 16rpx rgba(22, 93, 255, 0.18);
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  flex-shrink: 0;
  background: rgba(10, 10, 10, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -8rpx 40rpx rgba(0, 0, 0, 0.35);
}

.chat-container.cyber-wot {
  :deep(.cyber-chat-input) {
    flex: 1;
    min-width: 0;
    background: rgba(18, 18, 18, 0.94);
    border-radius: 999rpx;
    padding: 4rpx 8rpx 4rpx 12rpx;
    @include glow-border-purple;
  }

  :deep(.cyber-chat-input-inner) {
    color: $cyber-text !important;
    font-size: 28rpx;
  }

  :deep(.wd-input__prefix) {
    margin-right: 4rpx;
  }

  :deep(.wd-input__icon) {
    color: rgba(180, 180, 180, 0.9) !important;
  }

  :deep(.cyber-send-btn) {
    flex-shrink: 0;
    box-shadow: 0 0 24rpx rgba(22, 93, 255, 0.28);
  }

  :deep(.room-leave) {
    flex-shrink: 0;
  }
}
</style>
