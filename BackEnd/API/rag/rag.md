# RAG 接口

Base Path：`/api/rag`

## 1. 重建知识库

```http
POST /api/rag/reload
```

说明：执行 Milvus collection 创建或重建、JSONL 导入、向量生成和索引构建。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "ready": true,
    "message": "RAG 知识库已重建"
  },
  "timestamp": "2026-05-02T10:30:00"
}
```

## 2. 向量检索

```http
GET /api/rag/retrieve?question=泉州提线木偶有什么特点&topK=3
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| question | string | 是 | - | 用户问题 |
| topK | number | 否 | 3 | 返回检索片段数量 |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "chunk-1",
      "title": "泉州提线木偶",
      "score": 0.89,
      "content": "检索片段内容",
      "metadata": {
        "source": "standard_list"
      }
    }
  ],
  "timestamp": "2026-05-02T10:30:00"
}
```

## 3. Prompt 上下文预览

```http
GET /api/rag/prompt?question=泉州提线木偶有什么特点&topK=3
```

说明：返回已拼接检索资料后的上下文字符串，便于调试最终传给模型的 RAG 内容。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": "拼接后的上下文内容",
  "timestamp": "2026-05-02T10:30:00"
}
```
