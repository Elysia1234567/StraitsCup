<template>
  <view class="agent-container wot-theme-dark cyber-wot">
    <wd-tabs
      v-model="activeTab"
      slidable="always"
      :map-num="12"
      :line-width="28"
      :line-height="6"
      color="#165dff"
      inactive-color="#a0a0a0"
      :show-scrollbar="false"
      custom-class="cyber-tabs"
    >
      <wd-tab v-for="(t, i) in tabList" :key="i" :title="t">
        <view v-if="Number(activeTab) === i" class="agent-tab-body">
          <view v-if="loading" class="loading-tip">加载器灵列表…</view>
          <view v-else-if="!filteredAgents.length" class="loading-tip">暂无数据，请检查后端 /api/agents</view>
          <view v-else class="agent-grid">
            <view v-for="(item, index) in filteredAgents" :key="item.agentCode || index" class="agent-card">
              <image v-if="item.avatar" class="card-cover" :src="item.avatar" mode="aspectFill" />
              <view v-else class="card-avatar-fallback">
                <wd-icon name="user" size="40rpx" color="#0a0e18" />
              </view>
              <view class="card-name">{{ item.name }}</view>
              <view class="card-code">{{ item.agentCode }}</view>
              <view class="card-desc">{{ item.personality || '非遗器灵' }}</view>
              <wd-button
                type="primary"
                plain
                hairline
                size="small"
                icon="arrow-right"
                custom-class="card-action"
                @click="startWithAgent(item)"
              >
                用此器灵问答
              </wd-button>
            </view>
          </view>
        </view>
      </wd-tab>
    </wd-tabs>
  </view>
</template>

<script>
import { getAgents } from '@/api/backend.js'
import { setPendingAgentCodes, setActiveRoomId } from '@/utils/storage.js'

/** 与后端 agentCode 前缀一致：fz 福州、xm 厦门 … */
const TAB_LIST = ['全部', '福州', '厦门', '莆田', '三明', '泉州', '漳州', '南平', '龙岩', '宁德']
const TAB_PREFIX = [null, 'fz', 'xm', 'pt', 'sm', 'qz', 'zz', 'np', 'ly', 'nd']

export default {
  data() {
    return {
      activeTab: 0,
      tabList: TAB_LIST,
      agents: [],
      loading: false,
    }
  },
  computed: {
    filteredAgents() {
      const prefix = TAB_PREFIX[this.activeTab]
      if (!prefix) return this.agents
      const p = `${prefix}_`
      return this.agents.filter((a) => a.agentCode && String(a.agentCode).startsWith(p))
    },
  },
  onShow() {
    this.loadAgents()
  },
  methods: {
    async loadAgents() {
      this.loading = true
      try {
        const list = await getAgents()
        this.agents = Array.isArray(list) ? list : []
      } catch (e) {
        console.warn(e)
        this.agents = []
        uni.showToast({ title: '加载 Agent 失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    startWithAgent(item) {
      if (!item || !item.agentCode) return
      setActiveRoomId(null)
      setPendingAgentCodes([item.agentCode])
      uni.showToast({ title: '已选择器灵', icon: 'none' })
      uni.switchTab({ url: '/pages/chat/chat' })
    },
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

.loading-tip {
  padding: 40rpx 24rpx;
  font-size: 26rpx;
  color: $cyber-text-muted;
  text-align: center;
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
    font-size: 24rpx;
    padding: 0 14rpx;
  }

  :deep(.wd-tabs__nav-item.is-active .wd-tabs__nav-item-text) {
    text-shadow:
      0 0 10rpx rgba(22, 93, 255, 0.55),
      0 0 24rpx rgba(22, 93, 255, 0.28);
  }

  :deep(.wd-tabs__line) {
    box-shadow: 0 0 14rpx rgba(22, 93, 255, 0.45);
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
  padding: 20rpx 20rpx 18rpx;
  text-align: center;
  box-sizing: border-box;
  margin-bottom: 20rpx;
  @include cyber-content-card;
}

.card-cover {
  width: 100%;
  height: 200rpx;
  border-radius: $cyber-radius-md;
  margin-bottom: 14rpx;
  background: rgba(18, 18, 18, 0.95);
}

.card-avatar-fallback {
  width: 84rpx;
  height: 84rpx;
  border-radius: 50%;
  margin: 0 auto 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, $cyber-purple, #0e4bcc);
  border: 1rpx solid rgba(22, 93, 255, 0.65);
  box-shadow:
    0 0 20rpx rgba(22, 93, 255, 0.35),
    inset 0 0 14rpx rgba(255, 255, 255, 0.12);
}

.card-name {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 6rpx;
  @include neon-text-purple;
}

.card-code {
  font-size: 20rpx;
  color: $cyber-text-muted;
  margin-bottom: 10rpx;
  word-break: break-all;
}

.card-desc {
  font-size: 22rpx;
  line-height: 1.45;
  margin-bottom: 14rpx;
  @include neon-text-soft;
  text-align: left;
  max-height: 120rpx;
  overflow: hidden;
}

.cyber-wot :deep(.card-action) {
  box-shadow: 0 0 20rpx rgba(22, 93, 255, 0.22);
}
</style>
