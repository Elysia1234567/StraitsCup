# OmniSource 项目架构说明

> "同源"——基于多智能体(Multi-Agent)与AIGC的非遗文化数字生命共创平台

## 项目简介

海峡两岸及港澳地区有着"同宗同源"的文化底蕴，但传统非遗文化（如皮影、蜀绣、剪纸、木偶戏等）面临断代。本项目旨在用AIGC让非遗文化"活过来"，从静态展示转变为可交互的"数字生命"。

## 核心技术创新

| 特性 | 说明 |
|------|------|
| **多智能体协同** | 后台运行多个AI Agent（历史学家、匠人、游客），基于RAG学习非遗文献，自主讨论，用户可介入群聊 |
| **AIGC文生图/3D** | 用户输入文字，自动生成非遗风格艺术作品，结合Web3D在虚拟展馆展示 |
| **异步高并发** | Redis作为消息队列，解决AIGC生成耗时导致的阻塞问题 |
| **RAG知识库** | Spring AI + 向量数据库，确保数字人物回答严谨、不胡编乱造 |

## 后端文件夹架构

```
src/main/java/com/omnisource/
├── OmniSourceApplication.java       # 应用入口
│
├── config/                          # 配置类
│   ├── RedisConfig.java             # Redis配置（消息队列、缓存）
│   ├── WebSocketConfig.java         # WebSocket实时通信配置
│   ├── SpringAIConfig.java          # Spring AI配置
│   ├── MilvusConfig.java            # 向量数据库配置
│   └── AsyncConfig.java             # 异步线程池配置
│
├── controller/                      # 控制器层 - REST API
│   ├── AgentController.java         # 多智能体交互API
│   ├── AIGCController.java          # AIGC生成接口
│   ├── ChatController.java          # 群聊WebSocket接口
│   ├── ExhibitController.java       # 虚拟展馆接口
│   └── UserController.java          # 用户管理
│
├── service/                         # 业务逻辑层
│   ├── AgentService.java            # 多智能体服务
│   ├── AIGCService.java             # AIGC生成服务
│   ├── ChatService.java             # 群聊服务
│   ├── RagService.java              # RAG检索服务
│   ├── ExhibitService.java          # 展馆服务
│   └── impl/                        # 服务实现类
│
├── agent/                           # 多智能体核心模块
│   ├── base/                        # Agent基础框架
│   │   ├── BaseAgent.java           # Agent抽象基类
│   │   ├── AgentRole.java           # 角色定义
│   │   └── AgentMessage.java        # Agent间消息格式
│   ├── roles/                       # 具体角色Agent
│   │   ├── HistorianAgent.java      # 历史学家 - 负责文化背景讲解
│   │   ├── CraftsmanAgent.java      # 匠人 - 负责技艺细节说明
│   │   ├── TouristAgent.java        # 游客 - 代表用户提问互动
│   │   └── ModeratorAgent.java      # 主持人 - 协调讨论流程
│   └── orchestrator/                # Agent协调器
│       └── AgentOrchestrator.java   # 多Agent协作调度中心
│
├── rag/                             # RAG知识库模块
│   ├── document/                    # 文档处理（PDF/Word解析）
│   ├── embedding/                   # 文本向量化
│   └── retriever/                   # 向量检索器
│
├── aigc/                            # AIGC生成模块
│   ├── image/                       # 文生图服务
│   ├── model3d/                     # 3D模型生成
│   └── style/                       # 非遗风格定义（剪纸/水墨/皮影等）
│
├── entity/                          # 实体类（与数据库表对应）
│   ├── User.java                    # 用户
│   ├── Agent.java                   # 智能体配置
│   ├── ChatMessage.java             # 聊天消息
│   ├── GenerationTask.java          # AIGC生成任务
│   └── Artifact.java                # 数字艺术品
│
├── dto/                             # 数据传输对象
│   ├── request/                     # 请求DTO
│   └── response/                    # 响应DTO
│
├── mapper/                          # MyBatis映射接口
│
├── repository/                      # 数据访问层
│   ├── UserRepository.java
│   └── ArtifactRepository.java
│
├── task/                            # 异步任务处理
│   ├── AIGCTask.java                # AI生成任务执行器
│   └── TaskStatus.java              # 任务状态管理
│
├── websocket/                       # WebSocket实时通信
│   ├── ChatWebSocketHandler.java    # 群聊消息处理
│   └── StreamWebSocketHandler.java  # 流式响应（AI生成进度）
│
├── utils/                           # 工具类
│   ├── JwtUtil.java                 # JWT令牌工具
│   ├── FileUtil.java                # 文件处理工具
│   └── VoiceUtil.java               # 语音合成工具
│
└── enums/                           # 枚举定义
    ├── AgentType.java               # Agent类型
    ├── GenerationStatus.java        # 生成任务状态
    └── HeritageType.java            # 非遗类型（剪纸/蜀绣/皮影等）
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 基础框架 | Spring Boot 4.0.5 |
| 数据访问 | MyBatis + MySQL |
| 缓存/消息队列 | Redis |
| AI集成 | Spring AI |
| 向量数据库 | Milvus / Redis Stack |
| 实时通信 | WebSocket |
| 构建工具 | Maven |

## 服务器配置

- **Redis**: `118.190.206.152:6379` (无密码)
- **MySQL**: `118.190.206.152:3306/omni-source`
  - 用户名: `root`
  - 密码: `123456`

## 开发规范

1. **分层清晰**: Controller → Service → Repository，禁止跨层调用
2. **接口先行**: Service层先定义接口，再在impl包中实现
3. **配置集中**: 所有配置类统一放在config包
4. **常量枚举**: 状态码、类型等使用枚举，禁止魔法数字
5. **文档注释**: 类和方法必须添加JavaDoc注释

---
*项目创建日期: 2026.4.17*
