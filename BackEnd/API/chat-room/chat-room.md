# 聊天室接口

Base Path：`/api/chat-rooms`

当前控制器使用本地默认用户 `userId = 1`。

## 1. 获取当前用户聊天室

```http
GET /api/chat-rooms
```

响应：`data` 为 `ChatRoom[]`，字段见 [ChatRoom](../common/response.md#chatroom)。

## 2. 创建聊天室

```http
POST /api/chat-rooms
Content-Type: application/json
```

请求体：

```json
{
  "name": "福建非遗讨论室",
  "themeId": 1,
  "agentCodes": ["fz_shoushan_stone", "qz_string_puppet"]
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 否 | 聊天室名称 |
| themeId | number | 否 | 文化主题 ID |
| agentCodes | string[] | 否 | 初始 Agent 编码列表 |

响应：`data` 为创建后的 `ChatRoom`。

## 3. 获取聊天室详情

```http
GET /api/chat-rooms/{roomId}
```

响应：`data` 为 `ChatRoom`。

## 4. 解散聊天室

```http
DELETE /api/chat-rooms/{roomId}
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "timestamp": "2026-05-02T10:30:00"
}
```

## 5. 获取聊天室 Agent 成员

```http
GET /api/chat-rooms/{roomId}/agents
```

响应：`data` 为 `ChatRoomMember[]`。

## 6. 替换聊天室 Agent

```http
PUT /api/chat-rooms/{roomId}/agents/{memberId}
Content-Type: application/json
```

请求体：

```json
{
  "agentCode": "xm_bead_embroidery"
}
```

响应：`data` 为替换后的 `ChatRoomMember`。

业务校验失败时返回 `400`。

## 7. 移除聊天室 Agent

```http
DELETE /api/chat-rooms/{roomId}/agents/{memberId}
```

成功响应：`Result<Void>`。

业务校验失败时返回 `400`。

## 8. 分页获取历史消息

```http
GET /api/chat-rooms/{roomId}/messages?page=1&size=20
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | number | 否 | 1 | 页码 |
| size | number | 否 | 20 | 每页数量 |

响应：`data` 为 `ChatMessage[]`。

## 9. 获取最近历史消息

```http
GET /api/chat-rooms/{roomId}/messages/recent?limit=50
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| limit | number | 否 | 50 | 返回最近消息数量 |

响应：`data` 为 `ChatMessage[]`。
