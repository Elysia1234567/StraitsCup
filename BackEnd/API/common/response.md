# 通用响应与数据结构

## 统一响应

大多数 REST 接口返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-05-02T10:30:00"
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 业务状态码，通常与 HTTP 状态含义一致 |
| message | string | 响应消息 |
| data | any | 响应数据 |
| timestamp | string | 服务端响应时间 |

## 常见状态码

| code | 说明 |
| --- | --- |
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 无内容 |
| 400 | 请求参数错误或业务校验失败 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务端错误 |

## Agent

```json
{
  "id": 1,
  "agentCode": "fz_shoushan_stone",
  "name": "寿山石雕器灵",
  "avatar": "https://example.com/avatar.png",
  "roleType": "CULTURAL",
  "personality": "沉稳、博学",
  "promptTemplate": "...",
  "knowledgeScope": "...",
  "languageStyle": "...",
  "constraints": "...",
  "maxTokens": 1200,
  "temperature": 0.7,
  "topP": 0.9,
  "isPreset": 1,
  "sortOrder": 1,
  "status": 1,
  "createTime": "2026-05-02T10:30:00",
  "updateTime": "2026-05-02T10:30:00",
  "isDeleted": 0
}
```

## ChatRoom

```json
{
  "id": 1,
  "roomCode": "room_xxx",
  "userId": 1,
  "themeId": 1,
  "name": "福建非遗讨论室",
  "description": "string",
  "maxMembers": 6,
  "memberCount": 3,
  "messageCount": 12,
  "status": 1,
  "createTime": "2026-05-02T10:30:00",
  "updateTime": "2026-05-02T10:30:00",
  "isDeleted": 0
}
```

## ChatRoomMember

```json
{
  "id": 1,
  "roomId": 1,
  "memberType": "AGENT",
  "userId": null,
  "agentId": 1,
  "displayName": "寿山石雕器灵",
  "avatar": "https://example.com/avatar.png",
  "roleInRoom": "MEMBER",
  "lastSpeakTime": "2026-05-02T10:30:00",
  "speakCount": 3,
  "joinTime": "2026-05-02T10:30:00",
  "status": 1,
  "createTime": "2026-05-02T10:30:00",
  "updateTime": "2026-05-02T10:30:00",
  "isDeleted": 0
}
```

## ChatMessage

```json
{
  "id": 1,
  "roomId": 1,
  "messageType": "CHAT",
  "senderType": "AGENT",
  "senderId": "fz_shoushan_stone",
  "senderName": "寿山石雕器灵",
  "senderAvatar": "https://example.com/avatar.png",
  "content": "回答内容",
  "imageUrl": null,
  "replyToMessageId": null,
  "metadata": "{}",
  "isStream": 0,
  "streamId": null,
  "searchEnabled": 0,
  "searchResults": null,
  "createTime": "2026-05-02T10:30:00",
  "updateTime": "2026-05-02T10:30:00",
  "isDeleted": 0
}
```
