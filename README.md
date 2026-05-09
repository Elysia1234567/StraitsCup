# 同源 OmniSource

基于多智能体与 AIGC 的非遗文化数字生命共创平台。

OmniSource 面向福建九地市非遗文化展示、问答、共创和沉浸式浏览场景，将非遗项目组织为可对话、可协作、可生成图像、可进入 3D/WebGL 展示空间的文化 Agent。项目当前包含 Spring Boot 后端、Vue 3 展示端、UniApp 移动端，以及比赛说明书和展示材料。

## Core Features

- **多智能体非遗群聊**：用户可创建聊天室，选择最多 6 个文化 Agent。后端根据知识范围、冷却时间和上下文选择 1-3 个 Agent 回复。
- **RAG 事实增强**：默认读取 `Util/standardList.jsonl`，优先使用 Milvus 向量检索，失败时回退到本地相似度检索，降低文化问答幻觉。
- **AIGC 图像共创**：支持绑定指定 Agent 生图，自动结合 Agent 人设、原型图和用户描述生成图片，并写入聊天室历史。
- **WebSocket 流式体验**：聊天室通过 `/ws/chat` 推送 `AGENT_START`、`AGENT_CHUNK`、`AGENT_END`、`IMAGE` 等事件。
- **联网搜索补充**：多 Agent 问答支持 Tavily 搜索开关，用于补充 RAG 资料之外的信息。
- **语音识别通道**：`/ws/voice` 支持普通话和闽南语参数，基于 DashScope Fun-ASR 实时返回识别文本。
- **沉浸式前端**：Vue 3 前端包含福建地图入口、非遗画廊、Agent 聊天室、AIGC 生图面板、OGL 无限画廊和 Three.js 模型展示。
- **小程序端原型**：UniApp 端提供首页、画廊、聊天和 Agent 页面，用于移动端展示。

## Repository Structure

```text
.
├─ BackEnd/                         # Spring Boot 3 backend
│  ├─ API/                          # Backend API docs
│  ├─ src/main/java/com/omnisource
│  │  ├─ controller/                # REST API controllers
│  │  ├─ service/                   # Service interfaces and orchestration
│  │  ├─ service/impl/              # Business implementations
│  │  ├─ service/mcp/               # MCP-style tool implementations
│  │  ├─ Agents/                    # Fujian cultural agent definitions
│  │  ├─ websocket/                 # Chat and voice WebSocket handlers
│  │  ├─ mapper/                    # MyBatis mappers
│  │  ├─ entity/                    # Database entities
│  │  ├─ dto/                       # Request and response DTOs
│  │  └─ exception/                 # Unified exception handling
│  └─ src/main/resources
│     ├─ application.yml            # Main runtime configuration
│     └─ db/                        # Schema and seed SQL
├─ inspira-vue3/                    # Vue 3 + Vite demo frontend
├─ uniapp/tongyuan/                 # UniApp mobile client
├─ Util/                            # Scraping, cleaning, import scripts and JSONL dataset
├─ 01_作品简介一页/                 # Competition one-page introduction material
├─ 02_作品设计说明书_署名版/        # Signed design document material
└─ 03_作品设计说明书_匿名版/        # Anonymous design document material
```

## Tech Stack

| Layer | Stack |
| --- | --- |
| Backend | Java 17, Spring Boot 3.3.6, Spring AI 1.0.0-M6 |
| Data | MySQL, MyBatis, Redis |
| Vector Retrieval | Milvus Java SDK, Qwen/DashScope embedding API, local fallback retrieval |
| AI | OpenAI-compatible Qwen chat model, Qwen image generation, multimodal image understanding |
| Realtime | WebSocket, SSE, DashScope Fun-ASR |
| Storage | Aliyun OSS |
| Frontend | Vue 3, Vite, Tailwind CSS, ECharts, OGL, Three.js, GSAP |
| Mobile | UniApp |

## Backend Quick Start

Prerequisites:

- JDK 17
- Maven
- MySQL
- Redis
- Milvus, optional but recommended for vector retrieval
- DashScope/Qwen API key
- Aliyun OSS credentials if using image upload or image generation archive

Create a local `.env` in the repository root or `BackEnd/`. `application.yml` imports both locations.

```properties
DB_URL=jdbc:mysql://127.0.0.1:3306/omni-source?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=your_password

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

QIANWEN_API_KEY=your_qwen_key
QIANWEN_MODEL=qwen3.5-plus
QIANWEN_IMAGE_MODEL=qwen-image-2.0-pro

MILVUS_HOST=127.0.0.1
MILVUS_PORT=19530
MILVUS_DATABASE=default

ALIYUN_OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
ALIYUN_OSS_ACCESS_KEY_ID=your_oss_key_id
ALIYUN_OSS_ACCESS_KEY_SECRET=your_oss_key_secret
ALIYUN_OSS_BUCKET_NAME=your_bucket
ALIYUN_OSS_PUBLIC_BASE_URL=https://your_bucket.oss-cn-beijing.aliyuncs.com

TAVILY_API_KEY=your_tavily_key
DASHSCOPE_API_KEY=your_dashscope_key
```

Start backend:

```bash
cd BackEnd
mvn spring-boot:run
```

Run tests:

```bash
cd BackEnd
mvn test
```

Build:

```bash
cd BackEnd
mvn clean package
```

Backend defaults to port `8081`.

## Frontend Quick Start

### Vue 3 Web Frontend

```bash
cd inspira-vue3
npm install
npm run dev
```

Main views:

- Home page: Fujian map and non-heritage gallery.
- Agent page: chat rooms, Agent selection, WebSocket streaming chat, image generation.
- Infinite grid page: OGL/WebGL image gallery.
- Data page: Three.js model viewer.

### UniApp Client

```bash
cd uniapp/tongyuan
npm install
npm run dev:h5
```

For real-device testing, configure the API base URL as a LAN-accessible backend address instead of `localhost`.

## Dataset and Import Tools

`Util/standardList.jsonl` is the default RAG dataset. `RagServiceImpl` loads this file on startup, syncs to Milvus when available, and keeps local fallback retrieval ready for demos.

Useful scripts:

```bash
pip install -r requirements.txt
python Util/import_standard_list.py
python Util/SearchList.py
```

## API Documentation

Backend API documentation is in [BackEnd/API/README.md](BackEnd/API/README.md).

Important endpoints:

- `POST /api/chat`: one-shot multi-agent Q&A.
- `GET /api/agents`: list enabled heritage agents.
- `POST /api/chat-rooms`: create a persistent chat room.
- `WS /ws/chat?roomId={roomId}`: realtime room chat.
- `POST /api/aigc/image`: generate an image and optionally broadcast it into a room.
- `GET /api/rag/retrieve`: debug RAG retrieval.
- `GET /api/system-profile`: structured technical profile for demos.

## Demo Flow

1. Open the Vue homepage and click a city on the Fujian map.
2. Show the linked non-heritage gallery cards and flip-card descriptions.
3. Enter the Agent page and create a room with 2-3 cultural Agent roles.
4. Ask a question such as `你们一起介绍一下寿山石雕的历史和工艺特点`.
5. Toggle web search only when you need external supplementation.
6. Show streamed multi-agent replies and the persisted chat history.
7. Generate an Agent image from a prompt and show it entering the room as an image message.
8. Open the infinite gallery or 3D model page for visual presentation.
9. Use `/api/system-profile` or the API docs to explain backend architecture during defense.

## Engineering Notes

- Current chat room APIs use local user `userId = 1`; formal login/auth is not yet connected.
- `StreamWebSocketHandler` exists as a placeholder, but only `/ws/chat` and `/ws/voice` are registered.
- RAG remains usable without Milvus because local retrieval fallback is built in.
- Keep real keys, passwords, server IPs, and OSS credentials in `.env` or `application-local.yml`; do not commit them.
- The repository includes competition material folders. Anonymous submissions must be checked to ensure no school, author, teacher, or metadata identity information leaks.

## Competition Highlights

- The project is not just a single chatbot: it combines cultural knowledge base, multi-agent scheduling, RAG, image generation, WebSocket streaming, OSS archive, and WebGL display.
- The fallback retrieval and Redis-based task/session state improve demo stability.
- The system profile API and structured API docs make technical depth easier to explain in defense.
- The frontend gives judges visible interaction: map selection, gallery browsing, agent group chat, image creation, and 3D display.
