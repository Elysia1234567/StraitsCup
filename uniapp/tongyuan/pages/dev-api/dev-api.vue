<template>
  <scroll-view class="page" scroll-y>
    <view class="section">
      <view class="h">通用</view>
      <wd-input v-model="q" label="调试问题" placeholder="例如：寿山石雕" />
      <wd-button block plain hairline type="primary" @click="runSystemProfile">GET /api/system-profile</wd-button>
      <wd-button block plain hairline @click="runGetAgents">GET /api/agents</wd-button>
      <wd-button block plain hairline @click="runAigcPlain">GET /api/aigc/chat（纯文本）</wd-button>
      <wd-button block plain hairline @click="copyStreamUrl">复制 SSE /api/aigc/stream 地址</wd-button>
      <wd-button block plain hairline @click="runPostAigc">POST /api/aigc/chat</wd-button>
    </view>

    <view class="section">
      <view class="h">多智能体 REST</view>
      <wd-button block plain hairline @click="runPostMultiChat">POST /api/chat</wd-button>
      <wd-input v-model="sessionId" label="sessionId" placeholder="查询用，可选" />
      <wd-button block plain hairline @click="runGetMultiSession">GET /api/chat/{sessionId}</wd-button>
    </view>

    <view class="section">
      <view class="h">聊天室 REST</view>
      <wd-input v-model="roomId" label="roomId" placeholder="数字房间 ID" />
      <wd-button block plain hairline @click="runListRooms">GET /api/chat-rooms</wd-button>
      <wd-button block plain hairline @click="runGetRoom">GET /api/chat-rooms/{id}</wd-button>
      <wd-input v-model="memberId" label="memberId" placeholder="PUT/DELETE 成员用" />
      <wd-input v-model="agentCode" label="agentCode" placeholder="成员操作" />
      <wd-button block plain hairline @click="runRoomAgents">GET …/agents</wd-button>
      <wd-button block plain hairline @click="runRoomMessages">GET …/messages</wd-button>
      <wd-button block plain hairline @click="runRoomRecent">GET …/messages/recent</wd-button>
      <wd-button block plain hairline @click="runAddRoomAgent">POST …/agents 添加</wd-button>
      <wd-button block plain hairline @click="runReplaceRoomAgent">PUT …/agents/{m}</wd-button>
      <wd-button block plain hairline @click="runRemoveRoomAgent">DELETE …/agents/{m}</wd-button>
      <wd-button block plain hairline type="error" @click="runDeleteRoom">DELETE 解散房间</wd-button>
    </view>

    <view class="section">
      <view class="h">RAG</view>
      <wd-button block plain hairline type="error" @click="runRagReload">POST /api/rag/reload</wd-button>
      <wd-button block plain hairline @click="runRagRetrieve">GET /api/rag/retrieve</wd-button>
      <wd-button block plain hairline @click="runRagPrompt">GET /api/rag/prompt</wd-button>
    </view>

    <view class="section">
      <view class="h">AIGC 其它</view>
      <wd-input v-model="imageUrl" label="图片 URL" placeholder="多模态 / 生图参考" />
      <wd-button block plain hairline @click="runMultimodal">POST /api/aigc/multimodal</wd-button>
      <wd-input v-model="imgPrompt" label="生图 prompt" />
      <wd-button block plain hairline @click="runGenImage">POST /api/aigc/image</wd-button>
      <wd-input v-model="taskId" label="taskId" />
      <wd-button block plain hairline @click="runImageTask">GET /api/aigc/image/tasks/{taskId}</wd-button>
    </view>

    <view class="section">
      <view class="h">上传</view>
      <wd-button block plain hairline type="primary" @click="pickAndUpload">POST /api/upload/image</wd-button>
    </view>

    <view class="section">
      <view class="h">MCP</view>
      <wd-button block plain hairline @click="runMcpList">GET /api/mcp/tools</wd-button>
      <wd-input v-model="mcpName" label="工具名" />
      <wd-input v-model="mcpArgs" label="arguments JSON" placeholder="{}" />
      <wd-button block plain hairline @click="runMcpCall">POST /api/mcp/tools/{name}/call</wd-button>
    </view>

    <view class="section out">
      <view class="h">输出</view>
      <text class="out-text">{{ out }}</text>
    </view>
    <view class="footer" />
  </scroll-view>
</template>

<script>
import * as api from '@/api/backend.js'

export default {
  data() {
    return {
      q: '寿山石雕有什么特点',
      sessionId: '',
      roomId: '',
      memberId: '',
      agentCode: 'fz_shoushan_stone',
      imageUrl: 'https://via.placeholder.com/400',
      imgPrompt: '国风非遗插画',
      taskId: '',
      mcpName: '',
      mcpArgs: '{}',
      out: '',
    }
  },
  methods: {
    setOut(obj) {
      try {
        this.out = typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2)
      } catch {
        this.out = String(obj)
      }
    },
    async runSystemProfile() {
      try {
        this.setOut(await api.getSystemProfile())
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runGetAgents() {
      try {
        this.setOut(await api.getAgents())
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runAigcPlain() {
      try {
        this.setOut(await api.getAigcChatPlain(this.q || '你好'))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    copyStreamUrl() {
      const u = api.getAigcStreamUrl(this.q || '你好')
      uni.setClipboardData({ data: u })
      uni.showToast({ title: '已复制', icon: 'none' })
      this.setOut(u)
    },
    async runPostAigc() {
      try {
        this.setOut(await api.postAigcChat(this.q || '你好'))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runPostMultiChat() {
      try {
        this.setOut(
          await api.postMultiAgentChat({
            query: this.q || '你好',
            topK: 3,
            searchEnabled: false,
            agentCodes: this.agentCode ? [this.agentCode] : undefined,
          }),
        )
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runGetMultiSession() {
      if (!this.sessionId.trim()) {
        uni.showToast({ title: '填写 sessionId', icon: 'none' })
        return
      }
      try {
        this.setOut(await api.getMultiAgentSession(this.sessionId.trim()))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runListRooms() {
      try {
        this.setOut(await api.getChatRooms())
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runGetRoom() {
      const id = this.roomId.trim()
      if (!id) return uni.showToast({ title: '填写 roomId', icon: 'none' })
      try {
        this.setOut(await api.getChatRoom(id))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runRoomAgents() {
      const id = this.roomId.trim()
      if (!id) return uni.showToast({ title: '填写 roomId', icon: 'none' })
      try {
        this.setOut(await api.getChatRoomAgents(id))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runRoomMessages() {
      const id = this.roomId.trim()
      if (!id) return uni.showToast({ title: '填写 roomId', icon: 'none' })
      try {
        this.setOut(await api.getChatRoomMessages(id, 1, 20))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runRoomRecent() {
      const id = this.roomId.trim()
      if (!id) return uni.showToast({ title: '填写 roomId', icon: 'none' })
      try {
        this.setOut(await api.getChatRoomMessagesRecent(id, 30))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runAddRoomAgent() {
      const id = this.roomId.trim()
      if (!id || !this.agentCode.trim()) return uni.showToast({ title: 'roomId + agentCode', icon: 'none' })
      try {
        this.setOut(await api.postChatRoomAgent(id, this.agentCode.trim()))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runReplaceRoomAgent() {
      const id = this.roomId.trim()
      const mid = this.memberId.trim()
      if (!id || !mid || !this.agentCode.trim()) return uni.showToast({ title: 'roomId+memberId+agentCode', icon: 'none' })
      try {
        this.setOut(await api.putChatRoomAgent(id, mid, this.agentCode.trim()))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runRemoveRoomAgent() {
      const id = this.roomId.trim()
      const mid = this.memberId.trim()
      if (!id || !mid) return uni.showToast({ title: 'roomId+memberId', icon: 'none' })
      try {
        this.setOut(await api.deleteChatRoomAgent(id, mid))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    runDeleteRoom() {
      const id = this.roomId.trim()
      if (!id) return uni.showToast({ title: '填写 roomId', icon: 'none' })
      uni.showModal({
        title: '确认解散？',
        success: (r) => {
          if (!r.confirm) return
          api
            .deleteChatRoom(id)
            .then((d) => this.setOut(d))
            .catch((e) => this.setOut(e.message || String(e)))
        },
      })
    },
    async runRagReload() {
      try {
        this.setOut(await api.postRagReload())
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runRagRetrieve() {
      try {
        this.setOut(await api.getRagRetrieve(this.q || '测试', 3))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runRagPrompt() {
      try {
        this.setOut(await api.getRagPrompt(this.q || '测试', 3))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runMultimodal() {
      try {
        this.setOut(await api.postAigcMultimodal(this.imageUrl, this.q || '描述图片'))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runGenImage() {
      try {
        this.setOut(
          await api.postAigcImage({
            prompt: this.imgPrompt || '非遗海报',
            userId: 1,
          }),
        )
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runImageTask() {
      if (!this.taskId.trim()) return uni.showToast({ title: '填写 taskId', icon: 'none' })
      try {
        this.setOut(await api.getAigcImageTask(this.taskId.trim()))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    pickAndUpload() {
      uni.chooseImage({
        count: 1,
        success: async (res) => {
          const path = res.tempFilePaths[0]
          try {
            this.setOut(await api.uploadImage(path))
          } catch (e) {
            this.setOut(e.message || String(e))
          }
        },
      })
    },
    async runMcpList() {
      try {
        this.setOut(await api.getMcpTools())
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
    async runMcpCall() {
      const name = this.mcpName.trim()
      if (!name) return uni.showToast({ title: '填写工具名', icon: 'none' })
      let args = {}
      try {
        args = JSON.parse(this.mcpArgs || '{}')
      } catch {
        return uni.showToast({ title: 'arguments 需为 JSON', icon: 'none' })
      }
      try {
        this.setOut(await api.postMcpToolCall(name, args))
      } catch (e) {
        this.setOut(e.message || String(e))
      }
    },
  },
}
</script>

<style lang="scss" scoped>
@import '../../styles/cyber.scss';

.page {
  height: 100vh;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
  @include cyber-bg-grid;
}

.section {
  margin-bottom: 28rpx;
  padding: 22rpx 22rpx 26rpx;
  @include cyber-module-shell;
}

.h {
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 18rpx;
  @include neon-text-purple;
}

.section :deep(.wd-button) {
  margin-top: 12rpx;
}

.section :deep(.wd-input) {
  margin-bottom: 12rpx;
}

.out {
  min-height: 200rpx;
}

.out-text {
  font-size: 22rpx;
  color: $cyber-text-muted;
  white-space: pre-wrap;
  word-break: break-all;
}

.footer {
  height: calc(40rpx + env(safe-area-inset-bottom));
}
</style>
