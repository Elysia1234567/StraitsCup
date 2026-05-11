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

## ChatRoomInsightResponse

```json
{
  "roomId": 1,
  "roomName": "福建非遗讨论室",
  "agentCount": 3,
  "messageCount": 12,
  "latestQuestion": "寿山石雕有什么特点？",
  "latestAnswer": "寿山石雕以石质温润、刀法精细见长……",
  "latestAgentName": "寿山石雕器灵",
  "latestUpdateTime": "2026-05-02T10:30:00",
  "summary": "已围绕寿山石雕完成知识检索与综合回答。",
  "confidence": {
    "score": 0.92,
    "level": "高",
    "reason": "已基于 RAG、联网搜索或群聊记录生成证据链"
  },
  "evidenceSources": [
    {
      "id": "rag-1",
      "title": "寿山石雕知识条目",
      "provider": "RAG 知识库",
      "confidence": 0.89,
      "date": "2026-05-02",
      "excerpt": "寿山石雕是福州传统工艺之一……",
      "type": "RAG",
      "url": ""
    }
  ],
  "knowledgeTags": ["福建非遗讨论室", "寿山石雕器灵", "RAG", "多智能体"],
  "relationPaths": [
    { "key": "群聊主题", "value": "福建非遗讨论室" },
    { "key": "最新提问", "value": "寿山石雕有什么特点？" },
    { "key": "最近回复", "value": "寿山石雕器灵" },
    { "key": "证据来源", "value": "1 条" },
    { "key": "置信度", "value": "0.92 · 高" }
  ]
}
```
