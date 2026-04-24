# 多智能体接口草案

## 1. 发起多 Agent 问答

### `POST /api/chat`

请求体：

```json
{
  "sessionId": "sess_001",
  "query": "惠安女服饰有什么历史意义？",
  "heritageId": "huian_costume",
  "topK": 3,
  "agentRoles": ["historian", "craftsman", "tourist"]
}
```

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "sessionId": "sess_001",
    "query": "惠安女服饰有什么历史意义？",
    "finalAnswer": "综合来看，它既体现地域文化认同，也反映海洋生活方式。",
    "agentReplies": [
      {
        "role": "historian",
        "title": "历史学家视角",
        "content": "……",
        "references": ["chunk_001", "chunk_004"]
      }
    ],
    "retrievals": [
      {
        "chunkId": "chunk_001",
        "source": "standardList.jsonl",
        "score": 0.92,
        "content": "……"
      }
    ]
  }
}
```

## 2. 查询会话结果

### `GET /api/chat/{sessionId}`

用途：
- 查询当前会话的最近一次聚合结果
- 后续可用于断点恢复或多轮对话记录展示

## 3. 提交异步任务

### `POST /api/task`

请求体：

```json
{
  "taskType": "MULTI_AGENT_CHAT",
  "payload": {
    "sessionId": "sess_001",
    "query": "介绍一下福建木偶戏",
    "heritageId": "fujian_puppet"
  }
}
```

响应体：

```json
{
  "code": "SUCCESS",
  "message": "accepted",
  "data": {
    "taskId": "task_001",
    "status": "PENDING"
  }
}
```

## 4. 查询任务状态

### `GET /api/task/{taskId}`

响应字段建议：

- `taskId`
- `status`：`PENDING` / `RUNNING` / `SUCCESS` / `FAILED`
- `result`
- `errorCode`
- `errorMessage`

## 5. RAG 检索接口

### `POST /api/rag/search`

请求体：

```json
{
  "query": "惠安女服饰 起源",
  "topK": 3,
  "filters": {
    "heritageId": "huian_costume"
  }
}
```

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": [
    {
      "chunkId": "chunk_001",
      "source": "standardList.jsonl",
      "score": 0.92,
      "heritageId": "huian_costume",
      "content": "……"
    }
  ]
}
```

## 6. 向量库抽象要求

建议先抽象，不绑定具体实现：

```java
public interface VectorStore {
    void upsert(List<KnowledgeChunk> chunks);
    List<KnowledgeChunk> similaritySearch(String query, int topK, Map<String, Object> filters);
}
```

第一阶段可以先用：

- 假数据检索
- 本地内存检索
- 简单关键词匹配

第二阶段再替换为：

- Milvus
- Redis Stack Vector

## 7. 错误码建议

- `SUCCESS`
- `INVALID_PARAM`
- `SESSION_NOT_FOUND`
- `TASK_NOT_FOUND`
- `RETRIEVAL_FAILED`
- `LLM_CALL_FAILED`
- `INTERNAL_ERROR`
