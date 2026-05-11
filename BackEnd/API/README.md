# OmniSource Backend API

> Version: v2.2  
> Updated: 2026-05-10  
> Service base URL: `http://localhost:8081`  
> Source: `BackEnd/src/main/java/com/omnisource`

## Overview

OmniSource backend is a Spring Boot 3.3 service for the "同源" heritage AI platform. It provides:

- AIGC text chat, SSE streaming chat, multimodal image understanding, and image generation.
- Multi-agent heritage Q&A with RAG, optional Tavily web search, confidence assessment, and evidence chains.
- Chat room management with WebSocket streaming agent replies, historical room switching, room agent add/remove, and insight generation.
- RAG reload, retrieval debugging, and prompt preview with local JSONL fallback.
- MCP-style tool registry and tool invocation.
- OSS image upload and system profile output for demos or dashboards.
- Realtime voice recognition WebSocket based on DashScope Fun-ASR.

Most HTTP APIs return the unified `Result<T>` shape:

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-05-09T10:00:00"
}
```

Exceptions:

- `GET /api/aigc/chat` returns plain text.
- `GET /api/aigc/stream` returns `text/event-stream`.
- WebSocket endpoints use JSON events or binary audio frames.

## Endpoint Summary

| Method | Path | Module | Description |
| --- | --- | --- | --- |
| `GET` | `/api/aigc/chat` | AIGC | Plain text model chat. |
| `POST` | `/api/aigc/chat` | AIGC | JSON model chat, recommended for clients. |
| `GET` | `/api/aigc/stream` | AIGC | SSE streaming chat with RAG context. |
| `POST` | `/api/aigc/multimodal` | AIGC | Analyze an image URL with a question. |
| `POST` | `/api/aigc/image` | AIGC | Generate an image; can persist and broadcast into a room. |
| `GET` | `/api/aigc/image/tasks/{taskId}` | AIGC | Query image generation task status. |
| `GET` | `/api/agents` | Agent | List active heritage agents. |
| `GET` | `/api/agents/{code}` | Agent | Get one agent by code. |
| `POST` | `/api/chat` | Multi-agent | Run one multi-agent Q&A request. |
| `GET` | `/api/chat/{sessionId}` | Multi-agent | Read cached multi-agent result. |
| `GET` | `/api/chat-rooms` | Chat room | List local user's rooms. |
| `POST` | `/api/chat-rooms` | Chat room | Create a room with selected agents. |
| `GET` | `/api/chat-rooms/{roomId}` | Chat room | Get room detail. |
| `GET` | `/api/chat-rooms/{roomId}/insight` | Chat room | Get knowledge summary, key evidence, confidence, and provenance for the latest room state. |
| `DELETE` | `/api/chat-rooms/{roomId}` | Chat room | Dissolve a room. |
| `GET` | `/api/chat-rooms/{roomId}/agents` | Chat room | List room agent members. |
| `POST` | `/api/chat-rooms/{roomId}/agents` | Chat room | Add an agent to a room. |
| `PUT` | `/api/chat-rooms/{roomId}/agents/{memberId}` | Chat room | Replace one room agent member. |
| `DELETE` | `/api/chat-rooms/{roomId}/agents/{memberId}` | Chat room | Remove one room agent member. |
| `GET` | `/api/chat-rooms/{roomId}/messages` | Chat history | Page room messages. |
| `GET` | `/api/chat-rooms/{roomId}/messages/recent` | Chat history | Get recent room messages. |
| `PUT` | `/api/chat-rooms/{roomId}/messages/{messageId}/feedback` | Chat history | Mark message feedback as `1`, `0`, or `-1`. |
| `POST` | `/api/rag/reload` | RAG | Reload local JSONL and optionally sync vector store when Milvus is enabled. |
| `GET` | `/api/rag/retrieve` | RAG | Debug retrieval result. |
| `GET` | `/api/rag/prompt` | RAG | Preview retrieved context. |
| `POST` | `/api/upload/image` | Upload | Upload one image file to OSS. |
| `GET` | `/api/mcp/tools` | MCP | List available tools. |
| `POST` | `/api/mcp/tools/{name}/call` | MCP | Invoke one registered tool. |
| `GET` | `/api/system-profile` | System | Return architecture and capability profile. |
| `WS` | `/ws/chat?roomId={roomId}` | WebSocket | Realtime chat room event stream. |
| `WS` | `/ws/voice?dialect=mandarin|minnan` | WebSocket | Realtime PCM speech recognition. |

## AIGC

### `GET /api/aigc/chat`

Plain text chat.

Query:

| Name | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `message` | string | No | `你好` | User message. |

Example:

```bash
curl "http://localhost:8081/api/aigc/chat?message=介绍一下福州寿山石雕"
```

Response:

```text
寿山石雕是福州代表性传统工艺之一……
```

### `POST /api/aigc/chat`

JSON chat. This is better for UniApp and long user input.

Request:

```json
{
  "message": "介绍一下泉州提线木偶"
}
```

Response data is a string:

```json
{
  "code": 200,
  "message": "success",
  "data": "泉州提线木偶是福建泉州代表性传统戏剧和民间技艺……",
  "timestamp": "2026-05-09T10:00:00"
}
```

### `GET /api/aigc/stream`

SSE streaming chat. The backend first retrieves RAG chunks, then asks the model to answer based on those references.

Query:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `message` | string | No | `你好，请自我介绍` |

Example:

```bash
curl -N "http://localhost:8081/api/aigc/stream?message=寿山石雕有什么特点"
```

### `POST /api/aigc/multimodal`

Analyze an image URL.

Request:

```json
{
  "imageUrl": "https://example.com/image.png",
  "question": "请描述这张图片中的非遗元素"
}
```

### `POST /api/aigc/image`

Generate an image. When `roomId` and a valid `agentCode` are provided, the generated image is saved as a chat message and broadcast through `/ws/chat`.

Request:

```json
{
  "userId": 1,
  "roomId": 1001,
  "prompt": "生成一张寿山石雕风格海报",
  "style": "国风插画",
  "agentCode": "fz_shoushan_stone"
}
```

Response data:

```json
{
  "imageUrl": "https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/chatroom/example.png",
  "agentCode": "fz_shoushan_stone",
  "agentName": "寿山石雕器灵",
  "agentAvatar": "https://example.com/avatar.png",
  "sourceImageUrl": "https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/source/福州/寿山石雕.png",
  "messageId": "101"
}
```

### `GET /api/aigc/image/tasks/{taskId}`

Returns an `ImageGenerationTask` record, including task status, prompt, result URL, model name, and error message when available.

## Agents

### `GET /api/agents`

Lists active agents from the database.

### `GET /api/agents/{code}`

Example:

```bash
curl "http://localhost:8081/api/agents/fz_shoushan_stone"
```

Returns `404` in the response body when the agent code does not exist.

## Multi-Agent Q&A

### `POST /api/chat`

Runs one multi-agent Q&A request. This endpoint is different from the persistent chat room WebSocket flow: it returns a complete response containing each agent's answer, RAG references, conflict insight, confidence assessment, and evidence chain.

Request:

```json
{
  "sessionId": "demo-session-1",
  "query": "寿山石雕和德化瓷的文化价值有什么不同？",
  "heritageId": "fujian-demo",
  "topK": 3,
  "searchEnabled": false,
  "agentCodes": ["fz_shoushan_stone", "qz_dehua_porcelain"]
}
```

Important fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `query` | string | Yes | User question. |
| `sessionId` | string | No | If omitted, backend creates `sess_{uuid}`. |
| `topK` | number | No | RAG retrieval count. Default is `3`. |
| `searchEnabled` | boolean | No | Enables Tavily web search for RAG agents. |
| `agentCodes` | string array | No | Agent codes. Empty means default agents. |

### `GET /api/chat/{sessionId}`

Reads the latest cached result from Redis. Cache TTL is configured by `chat.multi-agent.session-ttl-days`.

## Chat Rooms

The current backend does not use formal login yet. `ChatRoomController` uses local user `userId = 1`.

### `GET /api/chat-rooms`

Lists local user's rooms.

### `POST /api/chat-rooms`

Create a room.

```json
{
  "name": "福州非遗圆桌",
  "themeId": 1,
  "agentCodes": ["fz_shoushan_stone", "fz_cork_scene", "fz_lacquerware"]
}
```

The configured maximum is `chat.room.max-agents`, currently `6`.

### Agent Member Management

Add an agent:

```http
POST /api/chat-rooms/{roomId}/agents
```

```json
{
  "agentCode": "xm_bead_embroidery"
}
```

Replace an agent:

```http
PUT /api/chat-rooms/{roomId}/agents/{memberId}
```

```json
{
  "agentCode": "qz_string_puppet"
}
```

Remove an agent:

```http
DELETE /api/chat-rooms/{roomId}/agents/{memberId}
```

### Messages

Page messages:

```http
GET /api/chat-rooms/{roomId}/messages?page=1&size=20
```

Recent messages:

```http
GET /api/chat-rooms/{roomId}/messages/recent?limit=50
```

Update feedback:

```http
PUT /api/chat-rooms/{roomId}/messages/{messageId}/feedback
```

```json
{
  "feedbackStatus": 1
}
```

`feedbackStatus` must be `1`, `0`, or `-1`.

### Room Insight

`GET /api/chat-rooms/{roomId}/insight` returns the current room insight payload used by the right-side panel in the web Agent page. The service combines:

- latest question and answer
- room agent members
- `chat_message.metadata`
- `chat_message.search_results`
- RAG retrieval fallback
- web search fallback

The response includes:

- `summary`
- `confidence` with `score`, `level`, and `reason`
- `evidenceSources`
- `knowledgeTags`
- `relationPaths`

This feature does not require a new database table in the current version. If you later want to persist every insight snapshot, you can add a dedicated snapshot table.

## RAG

RAG loads `Util/standardList.jsonl` by default. The backend now keeps local retrieval available first, and only syncs Milvus when `RAG_MILVUS_ENABLED=true`. If Milvus or the embedding service is unavailable, the service remains usable through local text scoring.

### `POST /api/rag/reload`

Reloads local JSONL and, when Milvus is enabled, incrementally syncs vector data.

### `GET /api/rag/retrieve`

```bash
curl "http://localhost:8081/api/rag/retrieve?question=柘荣剪纸&topK=3"
```

### `GET /api/rag/prompt`

Returns the context string that will be injected into a model prompt.

## Upload

### `POST /api/upload/image`

Multipart upload to OSS.

```bash
curl -X POST "http://localhost:8081/api/upload/image" \
  -F "file=@demo.png"
```

Response data:

```json
{
  "url": "https://example-bucket/OmniSource/demo.png",
  "filename": "demo.png"
}
```

## MCP Tools

### `GET /api/mcp/tools`

Lists registered tool descriptors.

### `POST /api/mcp/tools/{name}/call`

Request:

```json
{
  "arguments": {
    "query": "寿山石雕"
  }
}
```

Available tools depend on registered implementations under `service/mcp`, such as search, database query, OSS upload, task query, and image generation tools.

## System Profile

### `GET /api/system-profile`

Returns a structured demo profile:

- database and storage stack
- AI orchestration stack
- frontend rendering stack
- data flow
- innovation keywords

This endpoint is useful for defense presentations and frontend dashboards.

## WebSocket

### `/ws/chat?roomId={roomId}`

Realtime room chat endpoint.

Client sends:

```json
{
  "type": "CHAT",
  "senderType": "USER",
  "content": "你们一起介绍一下寿山石雕",
  "metadata": {
    "searchEnabled": false,
    "ragEnabled": true
  }
}
```

Supported inbound types:

- `CHAT`
- `IMAGE`
- `EMOJI`

Common outbound types:

- `SYSTEM`: user join/leave or system notice.
- `CHAT`: normal persisted message.
- `AGENT_START`: an agent begins streaming.
- `AGENT_CHUNK`: one streamed text chunk.
- `AGENT_END`: final event with persisted `messageId`.
- `IMAGE`: generated image message.
- `ERROR`: processing error.

### `/ws/voice?dialect=mandarin|minnan`

Realtime speech recognition endpoint.

- Client sends PCM binary audio frames.
- Text control message `{ "type": "stop" }` stops recognition.
- `dialect=minnan` uses the configured Minnan model when present; other values use Mandarin.

Outbound JSON events:

```json
{
  "type": "ready",
  "data": {
    "model": "fun-asr-realtime-2025-09-15",
    "dialect": "mandarin",
    "sampleRate": 16000,
    "time": "2026-05-09T10:00:00"
  }
}
```

```json
{
  "type": "transcript",
  "data": {
    "text": "寿山石雕",
    "sentenceEnd": true,
    "requestId": "..."
  }
}
```

## Runtime Configuration

Backend reads configuration from:

- `BackEnd/src/main/resources/application.yml`
- optional `application-local.yml`
- optional `.env` in `BackEnd/` or repository root

Required or commonly used environment variables:

| Variable | Description |
| --- | --- |
| `DB_URL` | MySQL JDBC URL. |
| `DB_USERNAME` | MySQL username. |
| `DB_PASSWORD` | MySQL password. |
| `REDIS_HOST` | Redis host. |
| `REDIS_PORT` | Redis port, default `6379`. |
| `REDIS_PASSWORD` | Redis password, optional. |
| `QIANWEN_API_KEY` | OpenAI-compatible DashScope/Qwen key. |
| `QIANWEN_BASE_URL` | Default `https://dashscope.aliyuncs.com/compatible-mode`. |
| `QIANWEN_MODEL` | Default `qwen3.5-plus`. |
| `QIANWEN_IMAGE_MODEL` | Default `qwen-image-2.0-pro`. |
| `RAG_MILVUS_ENABLED` | Enables Milvus sync and vector retrieval. Default `false`. |
| `MILVUS_HOST` | Milvus host. |
| `MILVUS_PORT` | Milvus port, default `19530`. |
| `ALIYUN_OSS_ACCESS_KEY_ID` | OSS access key id. |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | OSS access key secret. |
| `ALIYUN_OSS_BUCKET_NAME` | OSS bucket, default `java-ai-fzu`. |
| `TAVILY_API_KEY` | Tavily API key for optional web search. |
| `DASHSCOPE_API_KEY` | ASR key. Falls back to `QIANWEN_API_KEY` if omitted. |

## Run

```bash
cd BackEnd
mvn spring-boot:run
```

Build:

```bash
cd BackEnd
mvn clean package
```
