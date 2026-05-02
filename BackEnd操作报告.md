# BackEnd 操作报告

生成时间：2026-05-01

## 1. 目录清理

负责清理 `BackEnd` 下没有文件内容的空目录，让后端源码结构更干净。

已清理内容主要包括早期预留但未使用的空包，例如 `agent`、`aigc`、`rag`、`repository`、`task`、`static`、`templates` 等空目录。

保留了非空目录和有用途目录，例如 `API` 接口文档、`.mvn` Maven 配置、`.github` 工程配置、`target/classes` 编译产物、`src/main/java` 正式源码。

## 2. Agents 管理包

负责集中管理器灵 Agent 相关的公共逻辑。

位置：

```text
BackEnd/src/main/java/com/omnisource/Agents
```

当前包含：

```text
AgentDefinition.java         描述一个 Agent 的静态配置字段
AgentDefaults.java           管理默认启用的 Agent 编码
AgentPromptBuilder.java      管理 Agent 提示词拼装和聚合回答格式
ArtifactPainterAgent.java    管理绛影 Agent，人设为 RAG 关联型器灵形象设计
PlainGauzeSpiritAgent.java   管理轻芜 Agent，人设为无 RAG 沉浸式
QingJianAgent.java           管理青简 Agent，人设为 RAG 关联型
```

这样做的作用是让 Agent 的默认配置、提示词构建、RAG 资料拼接从业务服务中抽离出来，后续扩展到 27 个 Agent 时更容易维护。

## 3. AgentDefaults

负责维护系统默认启用的器灵 Agent。

当前默认 Agent：

```text
artifact_scholar  青简
artifact_painter  绛影
```

使用场景：

```text
1. 用户创建聊天室时没有指定 agentCodes
2. 用户调用多 Agent 聊天接口时没有指定 agentCodes
```

## 4. AgentPromptBuilder

负责把数据库中的 Agent 配置、用户问题、RAG 检索结果组装成大模型 Prompt。

它主要处理：

```text
1. Agent 人设
2. Agent 性格
3. Agent 知识范围
4. Agent 语言风格
5. Agent 约束条件
6. 当前文物或主题
7. 用户问题
8. RAG 检索资料
9. 输出格式要求
```

它支持两种提示词模式：

```text
普通 RAG 模式       拼接知识库资料，适合讲解型、考据型、设定生成型 Agent
无 RAG 沉浸模式    不拼接知识库，只按角色模板回答，适合轻芜这类记忆与感受型 Agent
```

如果 Agent 的 `prompt_template`、`knowledge_scope` 或 `constraints` 中包含 `NO_RAG` 或 `无RAG`，系统会自动使用无 RAG 沉浸模式。

普通 RAG 模式还支持可选联网搜索：请求里的 `searchEnabled=true` 时，系统会把联网搜索结果作为补充资料拼入 Prompt。资料优先级是 Agent 人设约束、RAG、联网搜索、大模型自身知识。

它还负责把多个 Agent 的回复聚合成最终回答。

## 5. 具体 Agent 定义

负责把具体 Agent 的画像用单独类管理。

当前已有：

```text
ArtifactPainterAgent.java
```

作用：管理“绛影”这个 RAG 关联型器灵形象设计 Agent。它会结合文物纹样、材质、色彩和文化意象，输出适合 AIGC 生图的器灵形象设定。

```text
PlainGauzeSpiritAgent.java
```

作用：管理“轻芜”这个无 RAG 沉浸式 Agent。它的回答只依赖角色模板和用户问题，不拼接知识库资料，适合表达记忆、感受、情绪和自我叙述。

```text
QingJianAgent.java
```

作用：管理“青简”这个 RAG 关联型 Agent。它会结合知识库资料回答，适合解释文物历史、文化背景、器物铭文和非遗知识。

## 6. MultiAgentChatServiceImpl

负责多 Agent 聊天流程编排。

它主要做：

```text
1. 创建或复用 sessionId
2. 判断本轮使用哪些 Agent
3. 调用 RAG 检索知识库
4. 调用 AgentPromptBuilder 构建提示词
5. 调用 Spring AI ChatClient 获取大模型回复
6. 返回每个 Agent 的独立回复和最终聚合回复
7. 缓存会话最近一次结果
```

它不再直接管理复杂提示词内容，提示词逻辑已经移动到 `Agents/AgentPromptBuilder.java`。

## 7. ChatRoomServiceImpl

负责聊天室创建、查询、解散和成员管理。

它主要做：

```text
1. 创建聊天室
2. 加入用户成员
3. 加入 Agent 成员
4. 查询用户聊天室
5. 查询房间成员
6. 解散聊天室
```

当创建聊天室时没有传入 `agentCodes`，它会使用 `AgentDefaults.DEFAULT_AGENT_CODES` 加入默认器灵。

## 8. Agent 数据表

负责保存 Agent 的可配置内容。

关键字段作用：

```text
agent_code       Agent 唯一编码，接口调用时使用
name             Agent 显示名称
avatar           Agent 头像地址
role_type        Agent 类型
personality      性格描述
prompt_template  人设提示词模板
knowledge_scope  知识范围
language_style   语言风格
constraints      回复约束
max_tokens       单次回复长度控制
temperature      创造性参数
top_p            采样参数
sort_order       展示排序
status           是否启用
```

新增 Agent 时主要维护数据库 `agent` 表，不需要频繁修改 Java 代码。

SQL 模板位置：

```text
BackEnd/src/main/resources/db/agent_template.sql
```

这个文件包含 `agent` 建表语句、`artifact_painter` 插入示例和通用新增 Agent 模板。

## 9. RAG 模块

负责让 Agent 在回答前先检索知识库。

它主要做：

```text
1. 读取 JSONL 知识库文件
2. 调用 embedding 模型生成向量
3. 写入 Milvus 向量数据库
4. 根据用户问题检索相关知识片段
5. 把检索结果交给 AgentPromptBuilder 拼进 Prompt
```

相关接口：

```text
POST /api/rag/reload      重建知识库
GET  /api/rag/retrieve    测试检索结果
GET  /api/rag/prompt      预览拼接后的上下文
```

## 10. AIGC 异步任务

负责处理耗时的文生图任务。

它主要做：

```text
1. 接收生图请求
2. 创建任务记录
3. 把任务状态写入 Redis
4. 后台异步执行生成逻辑
5. 更新数据库和 Redis 状态
6. 通过 WebSocket 推送进度和结果
```

这种结构可以避免 AI 生图长时间阻塞 HTTP 请求。

## 11. 配置文件

负责管理后端启动所需的数据库、Redis、Spring AI、千问、OSS、RAG、Milvus 等配置。

位置：

```text
BackEnd/src/main/resources/application.yml
```

建议后续把真实密钥、数据库密码、OSS 密钥改成环境变量，避免直接写在仓库中。
