# OmniSource 后端接口文档

> 版本：v2.0  
> 更新日期：2026-05-02  
> 对应代码：`BackEnd/src/main/java/com/omnisource/controller`

## 基础信息

- Base URL：`http://localhost:8081`
- 当前后端未接入正式登录鉴权，部分接口内部默认使用本地用户 `userId = 1`。
- REST 接口默认返回 JSON；`/api/aigc/stream` 返回 `text/event-stream`。

## 文档目录

| 模块 | 文档 | 说明 |
| --- | --- | --- |
| 通用约定 | [common/response.md](common/response.md) | 统一响应、错误码、通用实体字段 |
| AIGC | [aigc/aigc.md](aigc/aigc.md) | 普通问答、流式问答、多模态、图片生成 |
| Agent | [agent/agent.md](agent/agent.md) | 非遗智能体列表与详情 |
| 多 Agent 问答 | [chat/chat.md](chat/chat.md) | 多 Agent 聚合问答与会话查询 |
| 聊天室 | [chat-room/chat-room.md](chat-room/chat-room.md) | 聊天室、成员、历史消息 |
| RAG | [rag/rag.md](rag/rag.md) | 知识库重建、检索、Prompt 上下文预览 |
| 上传 | [upload/upload.md](upload/upload.md) | 图片上传 |
| WebSocket | [websocket/chat-websocket.md](websocket/chat-websocket.md) | 聊天室实时消息 |

## 接口总览

| 方法 | 路径 | 模块 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/aigc/chat` | AIGC | 普通 AI 问答 |
| GET | `/api/aigc/stream` | AIGC | SSE 流式 AI 问答，内部拼接 RAG 上下文 |
| POST | `/api/aigc/multimodal` | AIGC | 图片理解问答 |
| POST | `/api/aigc/image` | AIGC | 生成图片，可写入聊天室并广播 |
| GET | `/api/aigc/image/tasks/{taskId}` | AIGC | 查询图片生成任务 |
| GET | `/api/agents` | Agent | 获取启用的 Agent 列表 |
| GET | `/api/agents/{code}` | Agent | 按编码获取 Agent 详情 |
| POST | `/api/chat` | 多 Agent 问答 | 发起一次多 Agent 聚合问答 |
| GET | `/api/chat/{sessionId}` | 多 Agent 问答 | 查询会话最近一次结果 |
| GET | `/api/chat-rooms` | 聊天室 | 获取当前用户聊天室 |
| POST | `/api/chat-rooms` | 聊天室 | 创建聊天室 |
| GET | `/api/chat-rooms/{roomId}` | 聊天室 | 获取聊天室详情 |
| DELETE | `/api/chat-rooms/{roomId}` | 聊天室 | 解散聊天室 |
| GET | `/api/chat-rooms/{roomId}/agents` | 聊天室 | 获取聊天室 Agent 成员 |
| PUT | `/api/chat-rooms/{roomId}/agents/{memberId}` | 聊天室 | 替换聊天室 Agent |
| DELETE | `/api/chat-rooms/{roomId}/agents/{memberId}` | 聊天室 | 移除聊天室 Agent |
| GET | `/api/chat-rooms/{roomId}/messages` | 聊天室 | 分页获取历史消息 |
| GET | `/api/chat-rooms/{roomId}/messages/recent` | 聊天室 | 获取最近历史消息 |
| POST | `/api/rag/reload` | RAG | 重建知识库 |
| GET | `/api/rag/retrieve` | RAG | 向量检索 |
| GET | `/api/rag/prompt` | RAG | 预览拼接后的 RAG 上下文 |
| POST | `/api/upload/image` | 上传 | 上传图片到 OSS |
| WS | `/ws/chat?roomId={roomId}` | WebSocket | 聊天室实时消息通道 |

## 调试建议

1. 后端启动：在 `BackEnd/` 执行 `mvn spring-boot:run`。
2. 首次使用 RAG 前，先调用 `POST /api/rag/reload`。
3. 聊天室实时功能需要先创建聊天室，再连接 `ws://localhost:8081/ws/chat?roomId={roomId}`。
