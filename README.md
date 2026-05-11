# 同源 OmniSource

基于多智能体与 AIGC 的非遗文化数字生命共创平台。

OmniSource 面向福建九地市非遗文化展示、问答、共创和沉浸式浏览场景，将非遗项目组织为可对话、可协作、可生成图像、可进入 WebGL/3D 展示空间的文化 Agent。项目当前包含 Spring Boot 后端、Vue 3 展示端、UniApp 移动端、RAG 数据工具、真实 Agent 聊天室界面，以及比赛说明书和截图提示词材料。

## 核心能力

- **福建九地市非遗入口**：Vue 首页通过福建地图和 27 条本地画廊数据，把九地市非遗项目按城市分组展示。
- **真实多智能体聊天室**：用户可创建或切换历史聊天室，选择最多 6 个文化 Agent，并在房间内继续添加或删除 Agent；后端根据知识范围、上下文和冷却时间选择 1-3 个 Agent 流式回复。
- **知识增强与证据链**：`/api/chat-rooms/{roomId}/insight` 会汇总最近问题、Agent 回复、RAG 元数据、联网搜索结果和兜底检索，生成知识摘要、关键证据、综合置信度、知识标签和溯源信息。
- **RAG 事实增强**：默认读取 `Util/standardList.jsonl`，启动期先保证本地检索可用；Milvus 是可选向量增强，需显式设置 `RAG_MILVUS_ENABLED=true`。
- **AIGC 图像共创**：支持绑定聊天室内指定 Agent 生图，自动结合 Agent 人设、原型图和用户描述生成图片，并写入聊天室历史。
- **WebSocket 流式体验**：聊天室通过 `/ws/chat` 推送 `SYSTEM`、`CHAT`、`AGENT_START`、`AGENT_CHUNK`、`AGENT_END`、`IMAGE`、`ERROR` 等事件。
- **联网搜索与语音识别**：多 Agent 问答可按需启用 Tavily 搜索；`/ws/voice` 支持普通话与闽南语实时识别参数。
- **沉浸式前端**：Vue 3 前端包含首页地图、Agent 聊天室、Agent 库弹窗、非遗关系图谱、OGL 无限画廊和 Three.js 模型展示。
- **移动端原型**：UniApp 端提供首页、对话、智能体、图鉴和接口实验室页面，用于 H5/小程序方向演示。

## 目录结构

```text
.
├─ BackEnd/                         # Spring Boot 3 后端
│  ├─ API/                          # 后端 API 文档
│  ├─ src/main/java/com/omnisource
│  │  ├─ controller/                # REST API 控制器
│  │  ├─ service/impl/              # Agent、RAG、AIGC、聊天室等实现
│  │  ├─ service/mcp/               # MCP-style 工具实现
│  │  ├─ Agents/                    # 福建文化 Agent 定义与种子同步
│  │  ├─ websocket/                 # 聊天与语音 WebSocket
│  │  ├─ mapper/                    # MyBatis Mapper
│  │  ├─ entity/                    # 数据库实体
│  │  └─ dto/                       # 请求与响应 DTO
│  └─ src/main/resources
│     ├─ application.yml            # 主运行配置
│     └─ db/                        # Schema 与初始化 SQL
├─ inspira-vue3/                    # Vue 3 + Vite Web 展示端
├─ uniapp/tongyuan/                 # UniApp 移动端原型
├─ Util/                            # 数据抓取、清洗、JSONL 知识库工具
├─ 截图/                            # 截图素材与 AI 作图提示词
├─ 01_作品简介一页/                 # 作品简介材料
├─ 02_作品设计说明书_署名版/        # 署名版说明书
└─ 03_作品设计说明书_匿名版/        # 匿名版说明书
```

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17, Spring Boot 3.3.6, Spring AI 1.0.0-M6 |
| 数据 | MySQL, MyBatis, Redis |
| RAG | `standardList.jsonl` 本地检索, DashScope/Qwen Embedding, 可选 Milvus |
| AI | OpenAI-compatible Qwen chat, Qwen image generation, multimodal understanding |
| 实时能力 | WebSocket, SSE, DashScope Fun-ASR |
| 存储 | Aliyun OSS |
| Web 前端 | Vue 3, Vite, Tailwind CSS, ECharts, relation-graph, OGL, Three.js, GSAP |
| 移动端 | UniApp, Wot Design Uni |

## 后端启动

前置依赖：

- JDK 17
- Maven 3.9+
- MySQL
- Redis
- DashScope/Qwen API Key
- Aliyun OSS 配置，若使用上传、生图归档或 Agent 图片资源
- Milvus 可选，默认不启用

在仓库根目录或 `BackEnd/` 下创建 `.env`。`application.yml` 会导入 `BackEnd/src/main/resources/application-local.yml`、当前目录 `.env`、上级目录 `.env` 等配置。

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

# 可选：只有需要向量库同步时才打开
RAG_MILVUS_ENABLED=false
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

启动与构建：

```bash
cd BackEnd
mvn spring-boot:run
mvn test
mvn clean package
```

后端默认端口为 `8081`。

## Web 前端启动

```bash
cd inspira-vue3
npm install
npm run dev
```

主要页面：

- `/`：首页，福建地图入口、九地市非遗画廊、翻转卡片说明。
- `/agent`：真实聊天室、历史房间、Agent 增删、WebSocket 多智能体流式对话、知识摘要、关键证据、综合置信度、溯源信息、绑定 Agent 生图。
- `/graph`：非遗关系图谱，使用 `relation-graph/vue3` 和自定义节点渲染。
- `/infinite-grid`：OGL/WebGL 无限画廊展示。
- `/data`：独立 3D 模型展示页。

开发时若出现 `relation-graph/vue3` 解析失败，先确认在 `inspira-vue3/` 目录执行过 `npm install`，并检查 `node_modules/relation-graph` 是否存在。

## UniApp 启动

```bash
cd uniapp/tongyuan
npm install
npm run dev:h5
```

移动端页面来自 `pages.json`：

- `pages/index/index`：首页与聊天室入口。
- `pages/chat/chat`：自由问答与聊天室对话。
- `pages/agent/agent`：智能体矩阵。
- `pages/gallery/gallery`：非遗图鉴。
- `pages/dev-api/dev-api`：接口实验室，便于联调后端 API。

真机或局域网测试时，把 `VITE_API_BASE_URL` 配成可访问的后端地址。

## RAG 数据与工具

`Util/standardList.jsonl` 是当前默认知识库。`RagServiceImpl` 在启动时加载本地 JSONL，保证无 Milvus 环境下仍可检索；调用 `/api/rag/reload` 或定时任务时，若 `RAG_MILVUS_ENABLED=true` 才会尝试同步 Milvus。

常用命令：

```bash
pip install -r requirements.txt
python Util/SearchList.py --output projects.xlsx
python Util/import_standard_list.py
```

## API 文档

后端 API 详见 [BackEnd/API/README.md](BackEnd/API/README.md)。

重要接口：

- `GET /api/agents`：获取已启用文化 Agent。
- `POST /api/chat`：一次性多智能体问答。
- `GET /api/chat-rooms`：获取历史聊天室。
- `POST /api/chat-rooms`：创建持久聊天室。
- `POST /api/chat-rooms/{roomId}/agents`：向聊天室添加 Agent。
- `DELETE /api/chat-rooms/{roomId}/agents/{memberId}`：从聊天室移除 Agent。
- `GET /api/chat-rooms/{roomId}/insight`：生成知识摘要、关键证据、综合置信度、知识标签和溯源信息。
- `WS /ws/chat?roomId={roomId}`：聊天室实时事件流。
- `POST /api/upload/image`：上传用户图片到 OSS，供聊天或生图使用。
- `POST /api/aigc/image`：生成图片，并可广播进聊天室。
- `GET /api/rag/retrieve`：调试 RAG 检索。
- `GET /api/system-profile`：输出系统技术画像，适合答辩说明。

## 演示流程

1. 打开 Vue 首页，点击福建地图中的城市。
2. 展示底部非遗画廊和右侧翻转卡片说明。
3. 进入 Agent 页面，选择历史聊天室或新建聊天室。
4. 通过“添加 Agent”打开 Agent 库弹窗，选择成员加入当前房间；也可在成员列表中删除 Agent。
5. 输入 `你们一起介绍一下寿山石雕的历史和工艺特点`，展示多 Agent WebSocket 流式回复。
6. 查看右侧“知识增强与证据链”面板，展示知识摘要、关键证据、综合置信度和溯源信息。
7. 上传图片或选择聊天室内 Agent，输入视觉描述并生成图片。
8. 展示图片进入聊天流、OSS 归档和历史消息。
9. 打开 `/graph`、`/infinite-grid` 或 `/data` 展示关系图谱与沉浸式视觉页面。
10. 使用 `/api/system-profile` 辅助解释系统架构。

## 材料与截图

- `截图/` 保存 Web、移动端截图、Logo、校徽和 AI 作图提示词；当前 Agent 页面截图为 `截图/网页端Agent对话.jpg`，Agent 库弹窗截图为 `截图/网页端Agent智能体库.jpg`。
- `截图/README.md` 汇总了每张说明书配图的用途与提示词文件。
- `02_作品设计说明书_署名版/参赛作品说明书_署名版.md` 是当前署名版报告主体。

## 工程注意事项

- 当前聊天室 API 使用本地用户 `userId = 1`，正式登录鉴权尚未接入。
- `StreamWebSocketHandler` 是占位类，实际注册的 WebSocket 端点是 `/ws/chat` 和 `/ws/voice`。
- Milvus 默认关闭，RAG 仍可通过本地相似度检索工作；需要向量库增强时设置 `RAG_MILVUS_ENABLED=true`。
- 当前知识摘要、关键证据、综合置信度和溯源信息不需要新增数据库表，运行时由 `chat_message.metadata`、`chat_message.search_results`、RAG 检索结果和最近群聊记录综合生成。
- 若后续需要长期保存每次洞察结果快照，可新增 `chat_room_insight_snapshot` 表；当前版本暂不需要。
- 真实密钥、数据库密码、服务器 IP 和 OSS 凭据应放在 `.env` 或 `application-local.yml`，不要提交。
- 匿名版材料提交前需检查图片、文档属性和正文，避免泄露学校、作者、指导教师等身份信息。

## 参赛亮点

- 不只是单轮聊天：项目将历史聊天室、Agent 增删、知识库、Agent 调度、RAG、证据链、置信度、AIGC、生图入聊、WebSocket 流式通信、OSS 归档和 WebGL 展示串成闭环。
- 本地 RAG 回退与 Redis 状态缓存提升了现场演示稳定性。
- `system-profile` 接口和完整 API 文档便于答辩时说明工程深度。
- Web 与 UniApp 双端让评委能直观看到地图入口、画廊浏览、群聊协作、视觉生成和沉浸展示。
