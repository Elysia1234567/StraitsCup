<template>
  <view class="chat-container wot-theme-dark cyber-wot">
    <scroll-view
      class="message-list"
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
            <text class="bubble-text ai-text">{{ item.content }}</text>
          </view>
        </template>
        <template v-else>
          <view class="message-bubble user-bubble">
            <text class="bubble-text user-text">{{ item.content }}</text>
          </view>
          <view class="avatar user-avatar">
            <wd-icon name="user" size="36rpx" color="#1a0518" />
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
        placeholder-style="color: rgba(122,143,178,0.75);"
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
import { postAigcChat } from '@/utils/api.js'

export default {
  data() {
    return {
      inputText: '',
      sending: false,
      messageList: [
        {
          type: 'ai',
          content: '你好！我已接入服务器 AI，输入后点发送即可对话。（真机请配置 VITE_API_BASE_URL 为电脑局域网地址）',
        },
      ],
      scrollIntoView: '',
    }
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
  methods: {
    async sendMessage() {
      const t = this.inputText.trim()
      if (!t || this.sending) return
      this.messageList.push({ type: 'user', content: t })
      this.inputText = ''
      this.sending = true
      try {
        const reply = await postAigcChat(t)
        const text = reply && String(reply).trim() ? reply : '（模型返回为空）'
        this.messageList.push({ type: 'ai', content: text })
      } catch (e) {
        const msg = e && e.message ? e.message : '请求失败'
        this.messageList.push({
          type: 'ai',
          content: `暂时无法连接 AI 服务：${msg}\n请确认后端已启动（默认 8081），且本机/真机可访问 VITE_API_BASE_URL。`,
        })
        uni.showToast({ title: '发送失败', icon: 'none', duration: 2500 })
      } finally {
        this.sending = false
      }
    },
  },
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

.message-list {
  flex: 1;
  height: 0;
  padding: 20rpx;
  box-sizing: border-box;
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
  background: linear-gradient(160deg, $cyber-cyan, #0099aa);
  border: 1rpx solid rgba(0, 245, 255, 0.9);
  box-shadow: 0 0 18rpx rgba(0, 245, 255, 0.55);
}

.user-avatar {
  background: linear-gradient(160deg, $cyber-magenta, #aa0088);
  border: 1rpx solid rgba(255, 46, 230, 0.85);
  box-shadow: 0 0 18rpx rgba(255, 46, 230, 0.45);
}

.message-bubble {
  max-width: 60%;
  padding: 20rpx 22rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.5;
  word-break: break-word;
}

.ai-bubble {
  margin-left: 16rpx;
  background: rgba(8, 20, 45, 0.88);
  @include glow-border-cyan;
}

.user-bubble {
  margin-right: 16rpx;
  background: rgba(40, 5, 45, 0.75);
  @include glow-border-magenta;
}

.bubble-text {
  display: block;
}

.ai-text {
  @include neon-text-cyan;
}

.user-text {
  color: #ffe8ff;
  text-shadow:
    0 0 8rpx $cyber-magenta,
    0 0 16rpx rgba(255, 46, 230, 0.5);
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  flex-shrink: 0;
  background: rgba(5, 8, 20, 0.92);
  border-top: 1rpx solid rgba(0, 245, 255, 0.25);
  box-shadow: 0 -4rpx 24rpx rgba(0, 245, 255, 0.08);
}

.chat-container.cyber-wot {
  :deep(.cyber-chat-input) {
    flex: 1;
    min-width: 0;
    background: rgba(12, 18, 40, 0.9);
    border-radius: 36rpx;
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
    color: rgba(0, 245, 255, 0.65) !important;
  }

  :deep(.cyber-send-btn) {
    flex-shrink: 0;
    box-shadow: 0 0 16rpx rgba(0, 245, 255, 0.25);
  }
}
</style>

