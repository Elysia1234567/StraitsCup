<template>
  <view class="agent-container wot-theme-dark cyber-wot">
    <wd-tabs
      v-model="activeTab"
      slidable="always"
      :map-num="12"
      :line-width="28"
      :line-height="6"
      color="#00f5ff"
      inactive-color="#7a8fb2"
      :show-scrollbar="false"
      custom-class="cyber-tabs"
    >
      <wd-tab v-for="(t, i) in tabList" :key="i" :title="t">
        <view v-if="Number(activeTab) === i" class="agent-tab-body">
          <view class="agent-grid">
            <view v-for="(item, index) in agentList" :key="index" class="agent-card">
              <view class="card-avatar">
                <wd-icon :name="item.icon" size="40rpx" color="#061018" />
              </view>
              <view class="card-name">{{ item.name }}</view>
              <view class="card-desc">{{ item.desc }}</view>
              <wd-button type="primary" plain hairline size="small" icon="arrow-right" custom-class="card-action">
                启动
              </wd-button>
            </view>
          </view>
        </view>
      </wd-tab>
    </wd-tabs>
  </view>
</template>

<script>
export default {
  data() {
    return {
      activeTab: 0,
      tabList: ['推荐', '效率', '创作', '学习', '生活', '娱乐', '工作'],
      agentList: [
        { icon: 'computer', name: '编程助手', desc: '代码编写、调试、解释' },
        { icon: 'edit-outline', name: '文案大师', desc: '朋友圈、文案、演讲稿' },
        { icon: 'books', name: '学习助手', desc: '知识点讲解、习题解答' },
        { icon: 'home', name: '生活顾问', desc: '美食、旅行、生活建议' },
        { icon: 'chart-bubble', name: '设计助手', desc: 'UI设计、创意灵感' },
        { icon: 'goods', name: '商业策划', desc: '方案、计划书、营销' },
      ],
    }
  },
}
</script>

<style lang="scss" scoped>
@import '../../styles/cyber.scss';

.agent-container {
  min-height: 100vh;
  box-sizing: border-box;
  @include cyber-bg-grid;
}

.agent-tab-body {
  padding: 8rpx 0 24rpx;
}

.cyber-wot {
  :deep(.cyber-tabs.wd-tabs) {
    background: transparent;
  }

  :deep(.wd-tabs__nav) {
    background: transparent !important;
    padding: 8rpx 0 4rpx;
  }

  :deep(.wd-tabs__nav-item) {
    font-size: 26rpx;
    padding: 0 20rpx;
  }

  :deep(.wd-tabs__nav-item.is-active .wd-tabs__nav-item-text) {
    text-shadow:
      0 0 8rpx rgba(0, 245, 255, 0.85),
      0 0 20rpx rgba(0, 245, 255, 0.35);
  }

  :deep(.wd-tabs__line) {
    box-shadow: 0 0 12rpx rgba(0, 245, 255, 0.8);
    border-radius: 3rpx;
  }

  :deep(.wd-tabs__container) {
    background: transparent;
  }
}

.agent-grid {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 0 4rpx;
}

.agent-card {
  width: 340rpx;
  border-radius: 18rpx;
  padding: 28rpx 24rpx 22rpx;
  text-align: center;
  box-sizing: border-box;
  margin-bottom: 20rpx;
  background: rgba(8, 14, 32, 0.78);
  @include glow-border-cyan;
}

.card-avatar {
  width: 84rpx;
  height: 84rpx;
  border-radius: 50%;
  margin: 0 auto 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, $cyber-purple, #6b2d9e);
  border: 1rpx solid rgba(185, 103, 255, 0.9);
  box-shadow:
    0 0 20rpx rgba(185, 103, 255, 0.55),
    inset 0 0 14rpx rgba(255, 255, 255, 0.15);
}

.card-name {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 10rpx;
  @include neon-text-magenta;
}

.card-desc {
  font-size: 22rpx;
  line-height: 1.45;
  margin-bottom: 16rpx;
  @include neon-text-soft;
}

.cyber-wot :deep(.card-action) {
  box-shadow: 0 0 12rpx rgba(0, 245, 255, 0.2);
}
</style>
