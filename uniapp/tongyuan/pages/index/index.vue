<template>
  <view class="container wot-theme-dark cyber-wot">
    <wd-cell-group title="最近对话" :border="false" custom-class="cyber-cell-group">
      <wd-cell
        v-for="(item, index) in chatList"
        :key="index"
        :title="item.title"
        :label="item.content"
        :value="item.time"
        is-link
        :icon="item.icon"
        icon-size="40rpx"
        @click="goChat"
      />
    </wd-cell-group>

    <view class="fab-wrap">
      <wd-button type="primary" plain hairline round icon="add" custom-class="cyber-fab-btn" @click="goChat">
        新建对话
      </wd-button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      chatList: [
        { icon: 'chat', title: '豆包助手', content: '你好，有什么可以帮你？', time: '10:30' },
        { icon: 'computer', title: '编程助手', content: 'JavaScript代码编写完成', time: '昨天' },
        { icon: 'edit-outline', title: '文案助手', content: '帮我写一篇朋友圈文案', time: '周一' },
      ],
    }
  },
  methods: {
    goChat() {
      uni.switchTab({ url: '/pages/chat/chat' })
    },
  },
}
</script>

<style lang="scss" scoped>
@import '../../styles/cyber.scss';

.container {
  padding: 16rpx;
  min-height: 100vh;
  box-sizing: border-box;
  @include cyber-bg-grid;
}

.cyber-wot {
  :deep(.cyber-cell-group) {
    background: transparent;
  }

  :deep(.wd-cell-group__title) {
    padding: 12rpx 8rpx 20rpx;
    color: $cyber-text-muted;
    font-size: 24rpx;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }

  :deep(.wd-cell-group__body) {
    background: transparent;
  }

  :deep(.wd-cell) {
    margin-bottom: 16rpx;
    border-radius: 16rpx;
    overflow: hidden;
    background: $cyber-bg-panel !important;
    @include glow-border-cyan;
  }

  :deep(.wd-cell__title) {
    @include neon-text-cyan;
    font-weight: 600;
  }

  :deep(.wd-cell__label) {
    @include neon-text-soft;
    margin-top: 8rpx;
  }

  :deep(.wd-cell__value) {
    color: $cyber-magenta !important;
    text-shadow:
      0 0 8rpx rgba(255, 46, 230, 0.55),
      0 0 16rpx rgba(255, 46, 230, 0.2);
  }

  :deep(.wd-cell__icon) {
    filter: drop-shadow(0 0 6rpx rgba(0, 245, 255, 0.65));
  }

  :deep(.cyber-fab-btn) {
    min-width: 280rpx;
    box-shadow:
      0 0 20rpx rgba(0, 245, 255, 0.35),
      0 0 40rpx rgba(255, 46, 230, 0.12);
  }
}

.fab-wrap {
  position: fixed;
  bottom: 120rpx;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}
</style>
