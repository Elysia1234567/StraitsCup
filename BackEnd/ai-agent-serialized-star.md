# 多智能体Agent对话系统设计计划

## 背景与目标

前端需要实现多角色AI对话功能，要求：
- 区分不同Agent角色（历史学家、匠人、主持人等），各自有头像和回复风格
- 集成RAG知识库检索，确保非遗文化回答准确
- 使用HTTP SSE流式返回AI回复

## 技术选型

- **流式协议**: HTTP SSE (Server-Sent Events) - 用户指定
- **Agent框架**: 自研轻量编排器（非LangChain，减少依赖）
- **向量数据库**: Milvus（已规划）
- **Embedding**: 千问 text-embedding-v3
- **LLM**: 千问 Qwen-Max/Qwen-Turbo

## 核心架构

```
用户输入
    ↓
[POST] /api/agent/chat/stream (SSE)
    ↓
AgentOrchestrator 编排器
    ├──→ RAG检索器 → Milvus向量检索 → 相关文档片段
    ↓
角色路由（根据问题内容）
    ├──→ HistorianAgent（历史类问题）
    ├──→ CraftsmanAgent（工艺类问题）
    └──→ ModeratorAgent（引导/总结）
    ↓
Prompt组装（角色设定 + RAG上下文 + 对话历史）
    ↓
Spring AI ChatClient → 千问API
    ↓
SSE流式返回给前端
```

## 关键数据结构

### Agent角色定义
```java
public enum AgentRole {
    HISTORIAN("historian", "历史学家", "擅长考证历史渊源...", "avatar_historian.png"),
    CRAFTSMAN("craftsman", "非遗匠人", "精通制作工艺...", "avatar_craftsman.png"),
    MODERATOR("moderator", "主持人", "引导讨论...", "avatar_moderator.png");

    private final String code;
    private final String name;
    private final String description;
    private final String avatar;
}
```

### SSE事件格式
```json
{
    "event": "agent-message",
    "data": {
        "messageId": "msg-uuid",
        "sessionId": "sess-uuid",
        "agent": {
            "role": "historian",
            "name": "历史学家",
            "avatar": "/avatars/historian.png"
        },
        "content": "片段内容",
        "contentType": "chunk|complete|error",
        "timestamp": "2026-04-18T10:30:00"
    }
}
```

### 对话会话管理（Redis）
```
Key: agent:session:{sessionId}
Value: {
    "sessionId": "uuid",
    "userId": 123,
    "topic": "剪纸艺术",
    "activeAgents": ["historian", "craftsman"],
    "messages": [
        {"role": "user", "content": "..."},
        {"role": "historian", "content": "..."}
    ],
    "createTime": "...",
    "updateTime": "..."
}
TTL: 7天
```

## API接口设计

### 1. 创建对话会话
```http
POST /api/agent/session
Authorization: Bearer {token}
Content-Type: application/json

{
    "topic": "剪纸艺术",
    "initialContext": "用户想了解剪纸的历史和技法",
    "activeAgents": ["historian", "craftsman"]
}

Response: {
    "code": 200,
    "data": {
        "sessionId": "sess-uuid",
        "topic": "剪纸艺术",
        "activeAgents": [...],
        "createTime": "..."
    }
}
```

### 2. SSE流式对话（核心接口）
```http
GET /api/agent/chat/stream?sessionId=sess-uuid&message=用户问题
Authorization: Bearer {token}
Accept: text/event-stream

SSE Response:
event: start
data: {"sessionId": "...", "messageId": "...", "timestamp": "..."}

event: agent-thinking
data: {"agent": {"role": "historian", "name": "历史学家", ...}, "status": "retrieving"}

event: rag-result
data: {"retrievedDocs": 3, "sources": [...]}

event: agent-message
data: {"agent": {...}, "content": "片段1", "contentType": "chunk"}

event: agent-message
data: {"agent": {...}, "content": "片段2", "contentType": "chunk"}

event: agent-message
data: {"agent": {...}, "content": "", "contentType": "complete"}

event: end
data: {"sessionId": "...", "totalTokens": 256}
```

### 3. 获取对话历史
```http
GET /api/agent/session/{sessionId}/history?page=0&size=20
Authorization: Bearer {token}

Response: {
    "code": 200,
    "data": {
        "sessionId": "...",
        "messages": [
            {"messageId": "...", "role": "user", "content": "...", "timestamp": "..."},
            {"messageId": "...", "role": "historian", "agentName": "历史学家", "content": "...", "timestamp": "..."}
        ],
        "total": 50,
        "hasMore": true
    }
}
```

### 4. 获取Agent角色列表
```http
GET /api/agent/roles

Response: {
    "code": 200,
    "data": [
        {
            "role": "historian",
            "name": "历史学家",
            "description": "擅长考证非遗项目的历史渊源、文化背景",
            "avatar": "/avatars/historian.png",
            "capabilities": ["历史考证", "文化背景", "地域分布"]
        },
        {
            "role": "craftsman",
            "name": "非遗匠人",
            "description": "精通非遗技艺的制作工艺、材料工具、技法要点",
            "avatar": "/avatars/craftsman.png",
            "capabilities": ["制作工艺", "材料工具", "技法教学"]
        }
    ]
}
```

## 实现文件清单

### 后端（BackEnd）

| 文件 | 说明 |
|------|------|
| `agent/dto/AgentRole.java` | Agent角色枚举定义 |
| `agent/dto/AgentMessage.java` | Agent消息数据结构 |
| `agent/dto/ChatRequest.java` | 对话请求DTO |
| `agent/dto/ChatResponse.java` | 流式响应事件DTO |
| `agent/base/BaseAgent.java` | Agent抽象基类 |
| `agent/roles/HistorianAgent.java` | 历史学家Agent |
| `agent/roles/CraftsmanAgent.java` | 匠人Agent |
| `agent/roles/ModeratorAgent.java` | 主持人Agent |
| `agent/orchestrator/AgentOrchestrator.java` | Agent编排调度器 |
| `agent/service/AgentChatService.java` | 对话服务接口 |
| `agent/service/impl/AgentChatServiceImpl.java` | 对话服务实现 |
| `agent/controller/AgentController.java` | REST API控制器 |
| `rag/service/RagService.java` | RAG检索服务接口 |
| `rag/service/impl/RagServiceImpl.java` | RAG检索实现（Milvus） |
| `rag/dto/RetrievalResult.java` | 检索结果DTO |
| `config/SSEConfig.java` | SSE配置（超时、缓冲区） |

### 前端（FrontEnd）

| 文件 | 说明 |
|------|------|
| `src/api/agent.ts` | Agent API封装 |
| `src/stores/agent.ts` | Agent状态管理（Pinia） |
| `src/components/ChatWindow.vue` | 聊天窗口组件 |
| `src/components/AgentAvatar.vue` | Agent头像组件 |
| `src/components/MessageBubble.vue` | 消息气泡组件 |
| `src/views/AgentChat.vue` | 对话页面 |
| `src/utils/sse.ts` | SSE连接工具 |

## 核心流程时序图

```
┌─────────┐     ┌──────────┐     ┌──────────────┐     ┌─────────┐     ┌─────────┐
│  用户   │     │  前端    │     │ AgentController│    │Orchestrator│   │  千问API │
└────┬────┘     └────┬─────┘     └──────┬───────┘     └────┬────┘     └────┬────┘
     │               │                   │                  │               │
     │ 输入问题       │                   │                  │               │
     │──────────────>│                   │                  │               │
     │               │ GET /chat/stream  │                  │               │
     │               │──────────────────>│                  │               │
     │               │                   │ 1. RAG检索        │               │
     │               │                   │─────────┐         │               │
     │               │                   │<────────┘         │               │
     │               │                   │ 2. 路由Agent       │               │
     │               │                   │─────────────────>│               │
     │               │                   │                  │ 3. 组装Prompt │
     │               │                   │                  │──────────────>│
     │               │ SSE: start        │                  │               │
     │               │<──────────────────│                  │               │
     │               │                   │                  │ 4. 流式返回    │
     │               │ SSE: agent-message│<─────────────────────────────────│
     │               │<──────────────────│                  │ (chunk 1)     │
     │ 显示打字机效果  │                   │                  │               │
     │<──────────────│                   │                  │               │
     │               │ SSE: agent-message│<─────────────────────────────────│
     │               │<──────────────────│                  │ (chunk 2)     │
     │ 继续渲染       │                   │                  │               │
     │<──────────────│                   │                  │               │
     │               │ ...               │                  │ ...           │
     │               │ SSE: end          │<─────────────────────────────────│
     │               │<──────────────────│                  │               │
     │ 显示完成       │                   │                  │               │
     │<──────────────│                   │                  │               │
```

## 技术要点

### 1. SSE实现注意点
- 设置 `produces = MediaType.TEXT_EVENT_STREAM_VALUE`
- 使用 `SseEmitter` 或 `Flux<ServerSentEvent>`
- 配置超时：建议 5 分钟（AI生成可能较慢）
- 心跳机制：每 30 秒发送空行保持连接

### 2. Prompt工程
每个Agent有独立System Prompt：
```
【历史学家Agent】
你是非遗文化历史学家，擅长考证历史渊源。
请基于以下检索到的资料回答：
{{rag_context}}

回答要求：
- 严谨准确，引用史料
- 说明时间、地点、人物
- 如果不确定，明确说明

用户问题：{{user_question}}
```

### 3. RAG检索流程
```
用户问题 → Embedding向量化 → Milvus检索Top-5 → 重排序 → 注入Prompt
```

### 4. 多Agent协作策略
- **单Agent模式**：问题明确，直接路由给对应Agent
- **多Agent模式**：主持人Agent协调，各Agent轮流发言
- **自动路由**：根据问题内容关键词自动选择Agent（简单实现）

## 验收标准

- [ ] SSE接口能稳定流式返回AI回复
- [ ] 能区分不同Agent角色，各自有头像和回复风格
- [ ] RAG检索结果正确注入Prompt
- [ ] 对话历史能正确保存和查询
- [ ] 前端能正确渲染流式内容

## 后续扩展

1. **AIGC集成**：在Agent回复中触发文生图任务
2. **WebSocket群聊**：支持多人在同一对话中
3. **语音输入**：集成语音识别
