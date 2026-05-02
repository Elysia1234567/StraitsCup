# WebSocket 聊天接口

## 1. 建立连接

```text
ws://localhost:8081/ws/chat?roomId={roomId}
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| roomId | number | 是 | 聊天室 ID。缺少或格式错误时服务端会关闭连接 |

连接成功后，服务端会向房间广播一条 `SYSTEM` 消息，并带上在线人数 `onlineCount`。

## 2. 客户端发送消息

```json
{
  "type": "CHAT",
  "content": "泉州提线木偶有什么特点？",
  "imageUrl": null,
  "metadata": {
    "searchEnabled": true,
    "ragEnabled": true
  }
}
```

可发送的主要消息类型：

| type | 说明 |
| --- | --- |
| CHAT | 文本聊天 |
| IMAGE | 图片消息 |
| EMOJI | 表情消息 |

当前处理逻辑会读取：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| content | string | 文本内容 |
| imageUrl | string | 图片 URL |
| metadata.searchEnabled | boolean | 是否启用联网搜索 |
| metadata.ragEnabled | boolean | 是否启用 RAG |

## 3. 服务端推送消息结构

```json
{
  "type": "AGENT_END",
  "senderType": "AGENT",
  "senderId": "qz_string_puppet",
  "senderName": "泉州提线木偶器灵",
  "senderAvatar": "https://example.com/avatar.png",
  "content": "回答内容",
  "imageUrl": null,
  "roomId": 1,
  "streamId": "stream_xxx",
  "messageId": 12,
  "replyToMessageId": null,
  "metadata": {},
  "timestamp": "2026-05-02T10:30:00",
  "onlineCount": 1
}
```

消息类型枚举：

| type | 说明 |
| --- | --- |
| CHAT | 普通聊天消息 |
| AGENT_START | Agent 开始回复 |
| AGENT_CHUNK | Agent 流式片段 |
| AGENT_END | Agent 回复结束 |
| IMAGE | 图片消息 |
| EMOJI | 表情消息 |
| TYPING | 输入中 |
| PROGRESS | 进度 |
| SYSTEM | 系统消息 |
| ERROR | 错误消息 |

发送方枚举：

| senderType | 说明 |
| --- | --- |
| USER | 用户 |
| AGENT | 智能体 |
| SYSTEM | 系统 |
