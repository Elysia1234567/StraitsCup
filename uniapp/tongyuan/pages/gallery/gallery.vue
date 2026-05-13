<template>
  <view class="page wot-theme-dark cyber-wot">
    <!-- Discover 式：顶栏文案 + 首张焦点大卡 -->
    <view class="discover-head">
      <text class="discover-title">非遗图鉴</text>
      <text class="discover-sub">九市二十七项 · 本地影像</text>
    </view>

    <scroll-view class="scroll" scroll-y :show-scrollbar="false">
      <view v-if="featured" class="featured-card">
        <image class="featured-img" :src="featured.image" mode="aspectFill" lazy-load />
        <view class="featured-overlay">
          <text class="featured-label">{{ featured.label }}</text>
          <text class="featured-name">{{ featured.title }}</text>
        </view>
      </view>

      <view class="section-head">
        <text class="section-title">推荐浏览</text>
        <text class="section-more">{{ items.length }} 项</text>
      </view>

      <view class="grid">
        <view v-for="(item, index) in gridItems" :key="index" class="grid-card">
          <image class="grid-img" :src="item.image" mode="aspectFill" lazy-load />
          <view class="grid-overlay">
            <text class="grid-title">{{ item.title }}</text>
            <text class="grid-meta">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="list-rest">
        <view v-for="(item, index) in listRest" :key="'r-' + index" class="list-card">
          <image class="list-img" :src="item.image" mode="aspectFill" lazy-load />
          <view class="list-body">
            <view class="list-label">{{ item.label }}</view>
            <view class="list-title">{{ item.title }}</view>
            <text class="list-desc">{{ item.description }}</text>
          </view>
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
  computed: {
    featured() {
      return this.items.length ? this.items[0] : null
    },
    gridItems() {
      return this.items.slice(1, 7)
    },
    listRest() {
      return this.items.slice(7)
    },
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

.discover-head {
  flex-shrink: 0;
  padding: 28rpx 28rpx 12rpx;
}

.discover-title {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  letter-spacing: 0.02em;
  @include neon-text-purple;
}

.discover-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $cyber-text-muted;
}

.scroll {
  flex: 1;
  height: 0;
  padding: 0 24rpx;
  box-sizing: border-box;
}

.featured-card {
  position: relative;
  margin-bottom: 28rpx;
  border-radius: $cyber-radius-xl;
  overflow: hidden;
  @include cyber-content-card;
  box-shadow:
    0 12rpx 48rpx rgba(0, 0, 0, 0.4),
    0 0 60rpx rgba(22, 93, 255, 0.1);
}

.featured-img {
  width: 100%;
  height: 380rpx;
  display: block;
  background: rgba(18, 18, 18, 0.95);
}

.featured-overlay {
  position: absolute;
  left: 16rpx;
  right: 16rpx;
  bottom: 16rpx;
  padding: 20rpx 22rpx;
  border-radius: $cyber-radius-md;
  background: rgba(0, 0, 0, 0.62);
  border: 1rpx solid rgba(22, 93, 255, 0.22);
}

.featured-label {
  display: block;
  font-size: 22rpx;
  color: $cyber-neon-purple-soft;
  margin-bottom: 6rpx;
}

.featured-name {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: $cyber-text;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16rpx;
  padding: 0 4rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  @include neon-text-purple;
}

.section-more {
  font-size: 24rpx;
  color: $cyber-text-muted;
}

.grid {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.grid-card {
  position: relative;
  width: 338rpx;
  height: 260rpx;
  margin-bottom: 20rpx;
  border-radius: $cyber-radius-lg;
  overflow: hidden;
  @include cyber-content-card;
}

.grid-img {
  width: 100%;
  height: 100%;
}

.grid-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 14rpx 16rpx;
  background: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.82) 100%);
}

.grid-title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: $cyber-text;
}

.grid-meta {
  display: block;
  font-size: 20rpx;
  color: $cyber-neon-purple-soft;
  margin-top: 4rpx;
}

.list-rest {
  padding-top: 8rpx;
}

.list-card {
  display: flex;
  flex-direction: row;
  margin-bottom: 20rpx;
  border-radius: $cyber-radius-lg;
  overflow: hidden;
  @include cyber-content-card;
}

.list-img {
  width: 220rpx;
  height: 200rpx;
  flex-shrink: 0;
}

.list-body {
  flex: 1;
  padding: 16rpx 18rpx;
  min-width: 0;
}

.list-label {
  font-size: 20rpx;
  color: $cyber-neon-purple-soft;
  margin-bottom: 6rpx;
}

.list-title {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
  @include neon-text-purple;
}

.list-desc {
  font-size: 22rpx;
  line-height: 1.45;
  color: $cyber-text-muted;
  display: block;
  overflow: hidden;
}

.scroll-footer {
  height: calc(32rpx + env(safe-area-inset-bottom));
}
</style>
