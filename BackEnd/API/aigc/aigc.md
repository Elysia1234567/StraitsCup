# AIGC 接口

Base Path：`/api/aigc`

## 1. 普通 AI 问答

```http
GET /api/aigc/chat?message=你好
```

请求参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| message | string | 否 | 你好 | 用户问题 |

响应：该接口直接返回字符串，不包裹 `Result`。

```text
你好！很高兴认识你。
```

## 2. 流式 AI 问答

```http
GET /api/aigc/stream?message=泉州提线木偶有什么特点
```

响应类型：`text/event-stream;charset=UTF-8`

说明：接口会先调用 RAG 检索，构造带参考资料的 Prompt，再通过模型流式返回内容。

## 3. 图片理解问答

```http
POST /api/aigc/multimodal
Content-Type: application/json
```

请求体：

```json
{
  "imageUrl": "https://example.com/image.png",
  "question": "请描述这张图片"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| imageUrl | string | 是 | 图片 URL |
| question | string | 否 | 针对图片的问题 |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": "图片分析结果",
  "timestamp": "2026-05-02T10:30:00"
}
```

## 4. 生成图片

```http
POST /api/aigc/image
Content-Type: application/json
```

请求体：

```json
{
  "userId": 1,
  "roomId": 1,
  "prompt": "生成一张寿山石雕风格的器灵头像",
  "style": "国风插画",
  "agentCode": "fz_shoushan_stone"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | number | 否 | 用户 ID，默认 `1` |
| roomId | number | 否 | 聊天室 ID；传入且存在 Agent 时会保存图片消息并广播 |
| prompt | string | 是 | 图片生成提示词 |
| style | string | 否 | 图片风格 |
| agentCode | string | 否 | Agent 编码；传入后会拼接 Agent 设定和参考图 |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "imageUrl": "https://example.com/result.png",
    "agentCode": "fz_shoushan_stone",
    "agentName": "寿山石雕器灵",
    "agentAvatar": "https://example.com/avatar.png",
    "sourceImageUrl": "https://example.com/source.png",
    "messageId": "12"
  },
  "timestamp": "2026-05-02T10:30:00"
}
```

## 5. 查询图片生成任务

```http
GET /api/aigc/image/tasks/{taskId}
```

路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| taskId | string | 图片生成任务 ID |

响应数据 `data`：

```json
{
  "id": 1,
  "taskId": "task_xxx",
  "userId": 1,
  "roomId": 1,
  "prompt": "生成一张寿山石雕风格的器灵头像",
  "style": "国风插画",
  "status": "SUCCESS",
  "resultUrl": "https://example.com/result.png",
  "errorMessage": null,
  "progress": 100,
  "model": "wanx",
  "createTime": "2026-05-02T10:30:00",
  "updateTime": "2026-05-02T10:30:00"
}
```
