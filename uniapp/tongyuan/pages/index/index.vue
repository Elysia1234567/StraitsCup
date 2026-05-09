<template>
  <view class="container wot-theme-dark cyber-wot">
    <!-- Discover 式顶部：插画位 + 标题区 -->
    <view class="hero-card">
      <view class="hero-visual">
        <wd-icon name="chat" size="56rpx" color="rgba(196,181,253,0.95)" />
      </view>
      <view class="hero-copy">
        <text class="hero-kicker">OmniSource · 同源</text>
        <text class="hero-title">对话与聊天室</text>
        <text class="hero-desc">非遗器灵多智能体协作，支持 WebSocket 房间与 REST 问答。</text>
        <wd-button type="primary" round block custom-class="hero-cta" icon="arrow-right" @click="createRoom">
          新建聊天室
        </wd-button>
      </view>
    </view>

    <view class="module-shell">
      <wd-cell-group title="聊天室" :border="false" custom-class="cyber-cell-group">
        <wd-cell
          v-for="(item, index) in chatList"
          :key="item.id || index"
          :title="item.title"
          :label="item.content"
          :value="item.time"
          is-link
          icon="chat"
          icon-size="40rpx"
          @click="openRoom(item)"
        />
        <view v-if="!loadingRooms && !chatList.length" class="empty-tip">暂无聊天室，点上方按钮新建或打开「接口实验室」调试后端。</view>
      </wd-cell-group>
    </view>

    <view class="module-shell module-shell--compact">
      <wd-cell-group title="更多" :border="false" custom-class="cyber-cell-group">
        <wd-cell title="接口实验室" label="RAG / MCP / 上传 / AIGC 等" is-link icon="setting" @click="goDevApi" />
      </wd-cell-group>
    </view>

    <view class="fab-wrap">
      <wd-button type="primary" plain hairline round icon="add" custom-class="cyber-fab-btn" @click="createRoom">
        快捷新建
      </wd-button>
    </view>
  </view>
</template>

<script>
import { getChatRooms, postChatRoom } from '@/api/backend.js'
import { getMockRoomListForIndex } from '@/utils/mockChat.js'
import { setActiveRoomId } from '@/utils/storage.js'

function formatTime(iso) {
  if (!iso) return ''
  const s = String(iso)
  if (s.length >= 16) return s.slice(5, 16).replace('T', ' ')
  return s
}

export default {
  data() {
    return {
      loadingRooms: false,
      chatList: [],
    }
  },
  onShow() {
    this.loadRooms()
  },
  methods: {
    async loadRooms() {
      this.loadingRooms = true
      try {
        const rooms = await getChatRooms()
        const list = Array.isArray(rooms) ? rooms : []
        this.chatList = list.map((r) => ({
          id: r.id,
          roomId: r.id,
          title: r.name || `房间 ${r.id}`,
          content: r.description || `成员 ${r.memberCount ?? 0} · 消息 ${r.messageCount ?? 0}`,
          time: formatTime(r.updateTime || r.createTime),
        }))
      } catch (e) {
        console.warn(e)
        this.chatList = getMockRoomListForIndex()
        uni.showToast({ title: '已切换离线演示（模拟聊天记录）', icon: 'none', duration: 2600 })
      } finally {
        this.loadingRooms = false
      }
    },
    openRoom(item) {
      if (!item.roomId) return
      setActiveRoomId(item.roomId)
      uni.switchTab({ url: '/pages/chat/chat' })
    },
    goDevApi() {
      uni.navigateTo({ url: '/pages/dev-api/dev-api' })
    },
    createRoom() {
      uni.showModal({
        title: '新建聊天室',
        editable: true,
        placeholderText: '房间名称',
        success: async (res) => {
          if (!res.confirm) return
          const name = (res.content != null && String(res.content).trim()) || `房间-${Date.now()}`
          try {
            const room = await postChatRoom({ name, themeId: null })
            if (room && room.id) {
              setActiveRoomId(room.id)
              uni.showToast({ title: '已创建', icon: 'success' })
              uni.switchTab({ url: '/pages/chat/chat' })
            }
          } catch (e) {
            uni.showToast({ title: e.message || '创建失败', icon: 'none' })
          }
        },
      })
    },
  },
}
</script>

<style lang="scss" scoped>
@import '../../styles/cyber.scss';

.container {
  padding: 20rpx 24rpx;
  min-height: 100vh;
  box-sizing: border-box;
  padding-bottom: 160rpx;
  @include cyber-bg-grid;
}

.hero-card {
  @include cyber-module-shell;
  margin-bottom: 28rpx;
}

.hero-visual {
  height: 220rpx;
  @include cyber-hero-placeholder;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1rpx solid rgba(167, 139, 250, 0.15);
}

.hero-copy {
  padding: 28rpx 28rpx 32rpx;
}

.hero-kicker {
  display: block;
  font-size: 22rpx;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: $cyber-text-dim;
  margin-bottom: 12rpx;
}

.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.25;
  margin-bottom: 14rpx;
  @include neon-text-purple;
}

.hero-desc {
  display: block;
  font-size: 26rpx;
  line-height: 1.55;
  color: $cyber-text-muted;
  margin-bottom: 28rpx;
}

.cyber-wot :deep(.hero-cta) {
  box-shadow:
    0 8rpx 32rpx rgba(122, 66, 244, 0.45),
    0 0 48rpx rgba(167, 139, 250, 0.25);
}

.module-shell {
  @include cyber-module-shell;
  margin-bottom: 24rpx;
  padding-bottom: 8rpx;
}

.module-shell--compact {
  margin-bottom: 0;
}

.empty-tip {
  padding: 32rpx 24rpx 40rpx;
  font-size: 26rpx;
  color: $cyber-text-muted;
  line-height: 1.5;
}

.cyber-wot {
  :deep(.cyber-cell-group) {
    background: transparent;
  }

  :deep(.wd-cell-group__title) {
    padding: 20rpx 28rpx 12rpx;
    color: $cyber-text-muted;
    font-size: 24rpx;
    font-weight: 600;
    letter-spacing: 0.06em;
  }

  :deep(.wd-cell-group__body) {
    background: transparent;
    padding: 0 12rpx 12rpx;
  }

  :deep(.wd-cell) {
    margin-bottom: 12rpx;
    border-radius: $cyber-radius-md;
    overflow: hidden;
    background: $cyber-bg-panel-elevated !important;
    @include glow-border-purple;
  }

  :deep(.wd-cell:last-child) {
    margin-bottom: 0;
  }

  :deep(.wd-cell__title) {
    @include neon-text-purple;
    font-weight: 600;
    font-size: 30rpx;
  }

  :deep(.wd-cell__label) {
    @include neon-text-soft;
    margin-top: 8rpx;
  }

  :deep(.wd-cell__value) {
    color: $cyber-neon-purple-soft !important;
    font-size: 22rpx;
  }

  :deep(.wd-cell__icon) {
    filter: drop-shadow(0 0 8rpx rgba(167, 139, 250, 0.55));
  }

  :deep(.cyber-fab-btn) {
    min-width: 240rpx;
    box-shadow:
      0 0 28rpx rgba(122, 66, 244, 0.35),
      0 0 56rpx rgba(167, 139, 250, 0.12);
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
