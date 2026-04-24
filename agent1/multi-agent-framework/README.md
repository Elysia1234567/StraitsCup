# 多智能体后端基础框架

这个目录用于提前沉淀多智能体后端的设计与代码骨架，便于在正式接口尚未完成前先对齐角色 Prompt、接口结构和模块边界。

## 目录说明

- `prompts.md`：三个角色的系统 Prompt 初稿
- `api-spec.md`：聊天、任务、RAG 检索等接口草案
- `src/main/java/...`：可迁移到正式后端的 Java skeleton

## 当前目标

先打通以下最小链路的设计：

1. 用户发起问题
2. 调度器分发给三个 Agent
3. 每个 Agent 调用 RAG 检索上下文
4. 每个 Agent 基于角色 Prompt 生成回答
5. 聚合器返回统一结果

## 建议后续接入顺序

1. 将 `agent`、`llm`、`rag`、`task` 下接口迁入正式后端
2. 使用 `PromptTemplates` 中的角色 Prompt 先跑通假数据
3. 将 `MockRetrievalService` 替换为真实向量检索实现
4. 将 `MockLlmClient` 替换为 Spring AI 封装
5. 最后补 REST Controller 和 WebSocket/SSE
