# OmniSource 后端总接口文档

> 版本：v2.0  
> 更新日期：2026-05-03  
> 对应代码：`BackEnd/src/main/java/com/omnisource`  
> OpenAPI 文件：[`openapi.yaml`](openapi.yaml)

## 基础信息

- 本地 Base URL：`http://localhost:8081`
- 默认响应格式：`application/json`
- 流式接口：`GET /api/aigc/stream` 返回 `text/event-stream`
- 文件上传：`POST /api/upload/image` 使用 `multipart/form-data`
- WebSocket：`ws://localhost:8081/ws/chat?roomId={roomId}`
- 当前后端未接入正式登录鉴权，聊天室等部分接口内部默认使用本地用户 `userId = 1`。

## 统一响应结构

除 `GET /api/aigc/chat` 和 `GET /api/aigc/stream` 外，HTTP 接口通常返回统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-05-03T10:00:00"
}
```

常见业务状态码：`200` 成功，`400` 请求参数错误，`404` 资源不存在，`500` 服务端错误。实际 HTTP 状态通常仍为 `200`，业务结果以响应体中的 `code` 为准。

## 接口总览

| 方法 | 路径 | 模块 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/aigc/chat` | AIGC | 普通 AI 问答，直接返回纯文本 |
| GET | `/api/aigc/stream` | AIGC | SSE 流式 AI 问答，内部拼接 RAG 上下文 |
| POST | `/api/aigc/multimodal` | AIGC | 图片理解问答 |
| POST | `/api/aigc/image` | AIGC | 生成图片，可写入聊天室并广播 |
| GET | `/api/aigc/image/tasks/{taskId}` | AIGC | 查询图片生成任务 |
| GET | `/api/agents` | Agent | 获取启用的 Agent 列表 |
| GET | `/api/agents/{code}` | Agent | 按编码获取 Agent 详情 |
| POST | `/api/chat` | 多 Agent 问答 | 发起一次多 Agent 联合问答 |
| GET | `/api/chat/{sessionId}` | 多 Agent 问答 | 查询会话最近一次结果 |
| GET | `/api/chat-rooms` | 聊天室 | 获取当前用户聊天室列表 |
| POST | `/api/chat-rooms` | 聊天室 | 创建聊天室 |
| GET | `/api/chat-rooms/{roomId}` | 聊天室 | 获取聊天室详情 |
| DELETE | `/api/chat-rooms/{roomId}` | 聊天室 | 解散聊天室 |
| GET | `/api/chat-rooms/{roomId}/agents` | 聊天室 | 获取聊天室 Agent 成员 |
| PUT | `/api/chat-rooms/{roomId}/agents/{memberId}` | 聊天室 | 替换聊天室 Agent |
| DELETE | `/api/chat-rooms/{roomId}/agents/{memberId}` | 聊天室 | 移除聊天室 Agent |
| GET | `/api/chat-rooms/{roomId}/messages` | 聊天室 | 分页获取历史消息 |
| GET | `/api/chat-rooms/{roomId}/messages/recent` | 聊天室 | 获取最近历史消息 |
| POST | `/api/rag/reload` | RAG | 重建知识库 |
| GET | `/api/rag/retrieve` | RAG | 向量检索调试 |
| GET | `/api/rag/prompt` | RAG | 预览拼接后的 RAG 上下文 |
| POST | `/api/upload/image` | 上传 | 上传图片到 OSS |
| WS | `/ws/chat?roomId={roomId}` | WebSocket | 聊天室实时消息通道 |

## AIGC

### GET `/api/aigc/chat`

普通问答接口，直接返回字符串。

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `message` | string | 否 | `你好` | 用户输入 |

### GET `/api/aigc/stream`

流式问答接口，返回 `text/event-stream`。服务端会先根据 `message` 检索 RAG 资料，再将拼接后的 prompt 交给模型流式输出。

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `message` | string | 否 | `你好，请自我介绍` | 用户输入 |

### POST `/api/aigc/multimodal`

图片理解问答。

请求体：

```json
{
  "imageUrl": "https://example.com/image.png",
  "question": "请描述这张图片"
}
```

### POST `/api/aigc/image`

生成图片。传入 `roomId` 和有效 `agentCode` 时，生成结果会写入聊天室消息并通过 WebSocket 广播。

请求体：

```json
{
  "userId": 1,
  "roomId": 1001,
  "prompt": "生成一张寿山石雕风格海报",
  "style": "国风插画",
  "agentCode": "fz_shoushan_stone"
}
```

### GET `/api/aigc/image/tasks/{taskId}`

查询图片生成任务状态。

## Agent

### GET `/api/agents`

获取当前启用的 Agent 列表。

### GET `/api/agents/{code}`

按 Agent 编码查询详情，例如 `fz_shoushan_stone`。

## 多 Agent 问答

### POST `/api/chat`

发起一次多 Agent 联合问答。

请求体：

```json
{
  "sessionId": "optional-session-id",
  "query": "寿山石雕有哪些特点？",
  "heritageId": "fz_shoushan_stone",
  "topK": 3,
  "searchEnabled": true,
  "agentCodes": ["fz_shoushan_stone", "fz_lacquerware"]
}
```

### GET `/api/chat/{sessionId}`

查询指定会话最近一次多 Agent 问答结果。

## 聊天室

### GET `/api/chat-rooms`

获取当前用户的聊天室列表。

### POST `/api/chat-rooms`

创建聊天室。

请求体：

```json
{
  "name": "福州非遗讨论室",
  "themeId": 1,
  "agentCodes": ["fz_shoushan_stone", "fz_cork_scene"]
}
```

### GET `/api/chat-rooms/{roomId}`

获取聊天室详情。

### DELETE `/api/chat-rooms/{roomId}`

解散聊天室。

### GET `/api/chat-rooms/{roomId}/agents`

获取聊天室 Agent 成员列表。

### PUT `/api/chat-rooms/{roomId}/agents/{memberId}`

替换指定聊天室成员对应的 Agent。

请求体：

```json
{
  "agentCode": "xm_bead_embroidery"
}
```

### DELETE `/api/chat-rooms/{roomId}/agents/{memberId}`

移除聊天室成员。

### GET `/api/chat-rooms/{roomId}/messages`

分页获取历史消息。

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `page` | integer | 否 | `1` | 页码 |
| `size` | integer | 否 | `20` | 每页数量 |

### GET `/api/chat-rooms/{roomId}/messages/recent`

获取最近历史消息。

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `limit` | integer | 否 | `50` | 最近消息数量 |

## RAG

### POST `/api/rag/reload`

重建知识库：创建 Milvus collection、导入 JSONL 数据并建立索引。

### GET `/api/rag/retrieve`

向量检索调试接口。

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `question` | string | 是 | - | 检索问题 |
| `topK` | integer | 否 | `3` | 返回数量 |

### GET `/api/rag/prompt`

预览拼接后的 RAG 上下文。

查询参数同 `/api/rag/retrieve`。

## 上传

### POST `/api/upload/image`

上传图片到 OSS。

请求类型：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | 图片文件 |

返回数据：

```json
{
  "url": "https://example.com/upload.png",
  "filename": "upload.png"
}
```

## WebSocket

连接地址：

```text
ws://localhost:8081/ws/chat?roomId={roomId}
```

客户端发送消息示例：

```json
{
  "type": "CHAT",
  "senderType": "USER",
  "content": "介绍一下寿山石雕",
  "imageUrl": null,
  "metadata": {
    "searchEnabled": true,
    "ragEnabled": true
  }
}
```

服务端可能推送的消息类型包括：`SYSTEM`、`CHAT`、`AGENT_START`、`AGENT_CHUNK`、`AGENT_END`、`IMAGE`、`PROGRESS`、`ERROR`。

## 调试建议

1. 在 `BackEnd/` 目录执行 `mvn spring-boot:run` 启动服务。
2. 首次使用 RAG 前，先调用 `POST /api/rag/reload`。
3. 需要实时聊天时，先创建聊天室，再连接 `ws://localhost:8081/ws/chat?roomId={roomId}`。
4. 导入接口平台时，上传 `BackEnd/API/openapi.yaml`。
