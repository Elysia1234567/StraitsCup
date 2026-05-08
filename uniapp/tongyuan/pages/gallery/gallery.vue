<template>
  <view class="page wot-theme-dark cyber-wot">
    <view class="hero">
      <wd-icon name="picture" size="44rpx" color="rgba(0,245,255,0.9)" />
      <text class="hero-title">福建非遗图鉴</text>
      <text class="hero-sub">九市二十七项 · 本地图鉴</text>
    </view>

    <scroll-view class="scroll" scroll-y :show-scrollbar="false">
      <view v-for="(item, index) in items" :key="index" class="card">
        <image class="card-img" :src="item.image" mode="aspectFill" lazy-load />
        <view class="card-body">
          <view class="card-label">{{ item.label }}</view>
          <view class="card-title">{{ item.title }}</view>
          <text class="card-desc">{{ item.description }}</text>
        </view>
      </view>
      <view class="scroll-footer" />
    </scroll-view>
  </view>
</template>

<script>
import { GALLERY_ITEMS } from './gallery-data.js'

export default {
  data() {
    return {
      items: GALLERY_ITEMS,
    }
  },
}
</script>

<style lang="scss" scoped>
@import '../../styles/cyber.scss';

.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  @include cyber-bg-grid;
}

.hero {
  flex-shrink: 0;
  padding: 24rpx 28rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8rpx;
}

.hero-title {
  font-size: 36rpx;
  font-weight: 700;
  @include neon-text-cyan;
}

.hero-sub {
  font-size: 24rpx;
  color: $cyber-text-muted;
  letter-spacing: 0.08em;
}

.scroll {
  flex: 1;
  height: 0;
  padding: 0 16rpx;
  box-sizing: border-box;
}

.card {
  margin-bottom: 24rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background: $cyber-bg-panel;
  @include glow-border-cyan;
}

.card-img {
  width: 100%;
  height: 360rpx;
  display: block;
  background: rgba(6, 10, 28, 0.9);
}

.card-body {
  padding: 20rpx 22rpx 24rpx;
}

.card-label {
  font-size: 22rpx;
  color: $cyber-magenta;
  margin-bottom: 8rpx;
  text-shadow: 0 0 10rpx rgba(255, 46, 230, 0.35);
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
  @include neon-text-cyan;
}

.card-desc {
  font-size: 26rpx;
  line-height: 1.55;
  @include neon-text-soft;
  display: block;
}

.scroll-footer {
  height: calc(24rpx + env(safe-area-inset-bottom));
}
</style>
