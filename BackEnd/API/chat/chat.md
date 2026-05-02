# 多 Agent 问答接口

Base Path：`/api/chat`

## 1. 发起多 Agent 聚合问答

```http
POST /api/chat
Content-Type: application/json
```

请求体：

```json
{
  "sessionId": "optional-session-id",
  "query": "泉州提线木偶有什么特点？",
  "heritageId": "qz_string_puppet",
  "topK": 3,
  "searchEnabled": true,
  "agentCodes": ["qz_string_puppet", "fz_shoushan_stone"]
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| sessionId | string | 否 | 会话 ID；不传时由服务生成或按服务逻辑处理 |
| query | string | 是 | 用户问题，不能为空 |
| heritageId | string | 否 | 非遗主题 ID 或业务标识 |
| topK | number | 否 | RAG 检索数量 |
| searchEnabled | boolean | 否 | 是否允许联网搜索补充 |
| agentCodes | string[] | 否 | 参与回答的 Agent 编码列表；为空时使用默认 Agent |

响应：

```json
{
  "code": 200,
  "message": "多Agent问答成功",
  "data": {
    "sessionId": "session_xxx",
    "query": "泉州提线木偶有什么特点？",
    "heritageId": "qz_string_puppet",
    "finalAnswer": "综合回答内容",
    "agentReplies": [
      {
        "agentCode": "qz_string_puppet",
        "title": "泉州提线木偶视角",
        "content": "单个 Agent 回答",
        "references": ["chunk-1"],
        "searchUsed": false
      }
    ],
    "retrievals": [
      {
        "id": "chunk-1",
        "title": "泉州提线木偶",
        "score": 0.89,
        "content": "检索片段内容",
        "metadata": {}
      }
    ],
    "webSearchResult": null
  },
  "timestamp": "2026-05-02T10:30:00"
}
```

## 2. 查询会话最近一次结果

```http
GET /api/chat/{sessionId}
```

路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| sessionId | string | 会话 ID |

成功响应：`data` 结构同上。

未找到时返回：

```json
{
  "code": 404,
  "message": "未找到对应会话结果",
  "timestamp": "2026-05-02T10:30:00"
}
```
