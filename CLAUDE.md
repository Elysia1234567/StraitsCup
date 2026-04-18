# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

"同源"（OmniSource）是基于多智能体系统与 AIGC 的非遗文化数字生命共创平台。通过构建可交互、可对话的"数字传承人"，将静态文化遗产转化为具有生命力的数字生命体。

**核心创新**：
- 多智能体协同系统（历史学家、匠人、游客、主持人 Agent）
- RAG 检索增强生成，确保回答准确性
- AIGC 文生图 + Web3D 虚拟展馆
- 异步高并发架构（Redis 消息队列）

## 项目结构

```
StraitsCup/
├── BackEnd/          # Spring Boot 4.0.5 后端（项目名：OmniSource）
├── FrontEnd/         # Vue 3 前端（待开发）
├── Util/             # 工具脚本（Python 爬虫等）
└── requirements.txt  # Python 依赖
```

## 开发命令

### 后端开发

```bash
# 进入后端目录
cd BackEnd

# 构建项目（使用项目本地仓库，不影响全局）
mvn clean install -DskipTests -Dmaven.repo.local=.m2/repository

# 运行应用
java -jar target/OmniSource-0.0.1-SNAPSHOT.jar

# 或使用 Spring Boot Maven 插件
mvn spring-boot:run

# 单个测试
mvn test -Dtest=UserServiceTest

# 代码格式化（需配置 google-java-format）
mvn fmt:format
```

### Python 工具

```bash
# 安装依赖
python -m pip install -r requirements.txt

# 运行非遗项目爬虫
python Util/SearchList.py --output projects.xlsx --max-pages 10
```

## 后端架构

### 技术栈

- **框架**: Spring Boot 4.0.5 + Java 17
- **数据访问**: MyBatis + MySQL
- **缓存/消息队列**: Redis
- **认证**: JWT (Access Token 2h + Refresh Token 7d)
- **AI 集成**: Spring AI（千问 API，兼容 OpenAI 格式）
- **向量数据库**: Milvus（计划中）

### 分层架构

```
controller/        # REST API 控制器层
├── AuthController       # 用户认证（注册/登录/登出/刷新token）
├── UserController       # 用户管理
├── AgentController      # 多智能体交互（待实现）
├── AIGCController       # AIGC 生成（待实现）
└── ChatController       # WebSocket 群聊（待实现）

service/           # 业务逻辑层
├── impl/
│   └── UserServiceImpl   # 用户服务实现
└── (待实现: AgentService, AIGCService, ChatService)

config/            # 配置类
├── SecurityConfig         # Spring Security 配置
└── JwtAuthenticationFilter  # JWT 过滤器

dto/               # 数据传输对象
├── request/             # 请求 DTO
└── response/            # 响应 DTO

entity/            # 实体类（对应数据库表）
mapper/            # MyBatis 映射接口
utils/            # 工具类（JwtUtil, SecurityUtil, Result）
enums/            # 枚举定义
```

### 认证流程

1. **注册/登录**: 返回 `accessToken` (2h) + `refreshToken` (7d)
2. **API 请求**: 携带 `Authorization: Bearer {accessToken}` header
3. **Token 过期**: 使用 `refreshToken` 自动刷新
4. **登出**: Token 加入 Redis 黑名单

### 已实现的接口

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/refresh` - 刷新令牌
- `GET /api/user/me` - 获取当前用户信息

详细文档见 `BackEnd/API_DOCUMENTATION.md`

## 服务器配置

### 生产环境

```yaml
# MySQL
host: 118.190.206.152:3306
database: omni-source
username: root
password: 123456

# Redis
host: 118.190.206.152
port: 6379
password: (无)

# 千问 API
api-key: sk-a83fcef78af344cda2b94c0500c15fce
base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

配置文件: `BackEnd/src/main/resources/application.yml`

## 核心功能模块（待实现）

### 1. 多智能体系统

**Agent 角色定义**:
- `HistorianAgent` - 历史学家（提供严谨史实）
- `CraftsmanAgent` - 匠人（解释制作工艺）
- `TouristAgent` - 游客（代表大众提问）
- `ModeratorAgent` - 主持人（协调讨论流程）

**实现要点**:
- 继承 `BaseAgent` 抽象基类
- 使用 Spring AI 的 `ChatClient` 调用千问 API
- Agent 间通过 `AgentOrchestrator` 协调协作
- 对话历史存储在 Redis，支持多轮对话

### 2. RAG 知识库

**流程**:
1. 文档解析（PDF/Word → 文本）
2. 文本分块（chunking）
3. 向量化（embedding）
4. 存储到 Milvus 向量数据库
5. 检索：用户查询 → 向量检索 → Top-K 相关文档
6. 生成：检索结果 + Prompt → LLM 生成回答

**技术选型**:
- Spring AI + Milvus / Redis Stack
- Embedding 模型：千问 `text-embedding-v3`

### 3. AIGC 文生图

**实现**:
- 调用千问文生图 API（兼容 OpenAI 格式）
- 支持非遗风格：剪纸、水墨、皮影、刺绣等
- 异步任务队列（Redis List）处理耗时生成
- WebSocket 实时推送生成进度

### 4. WebSocket 实时通信

**用途**:
- 多 Agent 群聊流式响应
- AIGC 生成进度推送
- 3D 展馆实时更新

**配置**:
- `WebSocketConfig.java` - WebSocket 配置
- `ChatWebSocketHandler.java` - 群聊消息处理
- `StreamWebSocketHandler.java` - 流式 AI 响应

## 开发规范

### 后端开发

1. **分层清晰**: Controller → Service → Mapper，禁止跨层调用
2. **接口先行**: Service 层先定义接口，再在 `impl/` 包实现
3. **配置集中**: 所有配置类放在 `config/` 包
4. **枚举优先**: 状态码、类型使用枚举，禁止魔法数字
5. **JavaDoc**: 类和方法必须添加注释

### API 设计

- 统一响应格式: `{"code": 200, "message": "success", "data": {}}`
- RESTful 风格，资源命名使用复数
- 认证接口路径: `/api/auth/*`
- 需要认证的接口: `/api/user/*`, `/api/agents/*`, `/api/aigc/*`

### 数据库设计

- 表名使用小写下划线: `user`, `chat_message`, `artifact`
- 字段: `id` (主键), `create_time`, `update_time`
- 外键命名: `{related_table}_id`

## 前端开发（待开发）

### 技术栈

- Vue 3 + TypeScript
- Pinia 状态管理
- Vite 构建工具
- Three.js 3D 渲染
- WebSocket 实时通信

### 启动命令

```bash
cd FrontEnd
npm install
npm run dev      # 开发模式
npm run build    # 生产构建
```

## 常见问题

### Maven 依赖下载慢？

在 `~/.m2/settings.xml` 配置阿里云镜像：

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### 如何测试千问 API？

使用 `Util/LLMAPITest.java` 中的测试代码，确保配置 `application.yml` 中的 API key。

### 数据库连接失败？

检查 `application.yml` 中的 MySQL 配置，确保服务器 `118.190.206.152:3306` 可访问。

## 项目愿景

"同源者，非仅血脉之同，乃文化之共传承也"

以 AI 为舟，载非遗渡数字之海，连两岸四地之心。让千年文化在数字时代焕发新生，让每一个年轻人都能成为非遗文化的守护者与传承者。
