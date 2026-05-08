# 图像 AI 生成提示词合集

> 用途：将以下提示词交给专门的图像 AI 或设计排版 AI，用于生成说明书中的精美架构图、数据流图、数据库图和渲染流程图。建议统一风格：深海蓝 + 青绿 + 金色点缀，科技感但不过度炫光，中文文字清晰无乱码。

## 1. 系统总体架构图提示词

生成一张横版高清系统架构图，主题为“同源：基于多智能体与 AIGC 的非遗文化数字生命共创平台”。整体风格为海峡文化、非遗数字化、AI 科技感，配色使用深海蓝、青绿、金色和白色。画面采用分层架构，从上到下或从左到右展示六层：

1. 用户体验层：Vue 3、Vite、Tailwind CSS、ECharts、OGL、Three.js；包含福建地图、非遗画廊、多智能体聊天室、AI 生图、3D 模型展示。
2. 实时交互层：WebSocket、SSE、AGENT_START、AGENT_CHUNK、AGENT_END、IMAGE、PROGRESS。
3. 业务编排层：Spring Boot 3、Controller、Service、AgentOrchestrator、AgentPromptBuilder。
4. AI 能力层：Spring AI、ChatClient、Embedding、RAG、多模态分析、AIGC 图像生成。
5. 数据支撑层：MySQL、Redis、Milvus、Aliyun OSS。
6. 工程保障层：统一响应、全局异常、接口文档、系统画像接口、测试。

要求：模块边界清晰，箭头不交叉，文字准确，图标精致，避免密集堆叠。中间用一条主链路突出“用户输入 → Agent 调度 → RAG 检索 → 大模型生成 → WebSocket 流式返回 → 前端沉浸展示”。不要出现英文乱码，不要出现错误技术名。

## 2. AI 调度流程图提示词

生成一张“背后 AI 调度流程图”，横版，适合放入参赛作品说明书。标题为“多智能体 AI 调度机制”。图中用流程节点展示：

用户问题进入聊天室 → 保存用户消息 → 读取房间内 Agent → AgentOrchestrator 计算相关度 → Redis 冷却过滤 → 选择 1-3 个响应 Agent / 全员触发 → RagService 执行 Milvus 向量检索 → 本地兜底检索 → Tavily 联网补充（可选） → AgentPromptBuilder 组装人设、RAG、搜索、约束 → Spring AI ChatClient 流式生成 → WebSocket 推送 AGENT_START / AGENT_CHUNK / AGENT_END → ChatMessage 持久化。

视觉要求：使用中心流线结构，Agent 用多个拟人化小卡片表示，RAG 用知识库书卷或向量节点表示，Redis 用红色缓存图标表示，WebSocket 用闪电流线表示。强调“不是单模型直连，而是可控调度系统”。文字要清楚，箭头有方向，节点不要重叠。

## 3. RAG 数据流图提示词

生成一张“RAG 知识增强数据流图”。图中分为离线建库和在线检索两条路线：

离线建库：非遗资料 JSONL → 文档清洗 → 千问 Embedding → Milvus Collection `omnisource_rag` → 建索引。

在线检索：用户问题 → Query Embedding → Milvus COSINE Search → TopK 知识片段 → Prompt 拼接 → Agent 回答；旁边加一条兜底路径：Milvus / Embedding 不可用 → 本地标题、内容、地区、类别、级别相似度评分 → 返回候选知识。

要求：画面要体现知识库可信、资料可追溯、减少幻觉。可以使用书卷、向量点阵、数据库圆柱、检索放大镜、Prompt 文档等图标。配色为蓝绿科技风，重要兜底路径用金色虚线标识。

## 4. 数据库 ER 图提示词

生成一张精美数据库 ER 图，主题为“OmniSource 数据库核心模型”。使用现代软件架构图风格，背景浅色或深色均可，但文字必须清晰。

包含 6 张核心表：

1. `agent`：id, agent_code, name, avatar, role_type, personality, prompt_template, knowledge_scope, language_style, constraints, max_tokens, temperature, top_p, status。
2. `cultural_theme`：id, theme_code, name, category, cover_image, region, era, knowledge_base。
3. `chat_room`：id, room_code, user_id, theme_id, name, member_count, message_count, status。
4. `chat_room_member`：id, room_id, member_type, user_id, agent_id, display_name, avatar, speak_count。
5. `chat_message`：id, room_id, message_type, sender_type, sender_id, sender_name, content, image_url, stream_id, is_stream, metadata。
6. `image_generation_task`：id, task_id, user_id, room_id, prompt, style, status, result_url, progress, model。

关系：

- `chat_room` 1:N `chat_room_member`
- `chat_room` 1:N `chat_message`
- `agent` 1:N `chat_room_member`
- `chat_room` 1:N `image_generation_task`
- `cultural_theme` 1:N `chat_room`

要求：表结构不要太挤，可以只展示核心字段；关系线清晰；突出 `agent` 是数字生命配置中心，`chat_message` 是文化记忆层，`image_generation_task` 是 AIGC 任务追踪层。

## 5. AIGC 图像共创闭环图提示词

生成一张“AIGC 图像共创闭环”流程图。画面中展示用户选择某个文化 Agent，输入画面描述，系统读取 Agent 名称、编码、性格、头像、原型参考图 URL，构造生图 Prompt，调用图像模型，生成图片，下载图片，上传 OSS，写入 `image_generation_task` 和 `chat_message`，最后以图片消息回到聊天室。

流程节点：

选择 Agent → 输入创作需求 → Agent 文化原型注入 → 图像模型生成 → OSS 归档 → 图片消息广播 → 用户继续讨论 → 二次共创。

视觉风格：海峡文化 + 非遗工艺 + AI 生成艺术。可以使用画笔、图像卡片、云存储、聊天气泡、Agent 头像、循环箭头等元素。强调“问答 - 创作 - 再阐释 - 沉淀”的循环。

## 6. 前端 3D / WebGL 渲染架构图提示词

生成一张“前端 3D / WebGL 渲染架构图”。图中展示三个视觉模块：

1. 首页文化入口：ECharts 福建地图 + CircularGallery + FlipCard + ProgressiveBlur。
2. OGL 无限 3D 画廊：InfiniteGridClass、3×3 Tile Groups、Canvas 2D Texture、Raycast Interaction、GSAP Inertia、PostProcess Shader、DisposalManager。
3. Three.js 模型展示：WebGLRenderer、Scene、Camera、OrbitControls、GLTFLoader、OBJLoader、MTLLoader、Light、Model Fit。

要求：图形要有现代前端工程感，类似高级技术白皮书插图。用三列结构展示，每列有小型可视化示意：地图、网格、3D 模型。底部用箭头表示“非遗图片 / 模型资产 → 纹理生成 / 模型加载 → WebGL 渲染 → 用户交互”。避免线条重叠。

## 7. 实时通信时序图提示词

生成一张 WebSocket 实时通信时序图，角色从左到右为：用户浏览器、ChatWebSocketHandler、AgentChatService、AgentOrchestrator、RagService、Spring AI、MySQL、WebSocketSessionManager。

时序：

用户发送 CHAT → 后端保存用户消息 → 广播用户消息 → 查询房间 Agent → 选择响应 Agent → 广播 AGENT_START → 调用 RAG → 构建 Prompt → Spring AI 返回流式 chunk → 每个 chunk 广播 AGENT_CHUNK → 完成后保存 Agent 消息 → 广播 AGENT_END。

要求：生成正式 UML sequence diagram 风格，但视觉更精美，使用深色背景和青绿色时序线。文字清晰，不要省略关键消息名。

## 8. 部署拓扑图提示词

生成一张系统部署拓扑图，展示浏览器前端、Spring Boot 后端、MySQL、Redis、Milvus、阿里云 OSS、千问兼容模型服务、Tavily 搜索服务之间的连接关系。

节点说明：

- Browser / Vue3：页面、WebSocket、3D 渲染。
- Spring Boot：REST API、WebSocket、Agent 调度、RAG、AIGC。
- MySQL：Agent、聊天室、消息、任务。
- Redis：冷却状态、任务缓存。
- Milvus：向量知识库。
- OSS：图片资源和生成图归档。
- Model API：文本生成、Embedding、视觉理解、图像生成。
- Search API：联网资料补充。

要求：像云架构图，节点有图标，连接线标注协议或用途：HTTP REST、WebSocket、SQL、Redis Ops、Vector Search、OSS Upload、Model API。布局清晰，适合放在说明书中。

## 统一负面提示词

不要出现错别字，不要出现乱码，不要出现错误技术名称，不要让模块重叠，不要线条混乱，不要把 MySQL、Redis、Milvus、OSS 画成同一个数据库，不要把前端和后端混成一层，不要生成卡通低龄风，不要过度装饰，不要让背景影响文字可读性，不要出现无关人物照片。
