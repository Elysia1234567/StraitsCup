# Agent 接口

Base Path：`/api/agents`

## 1. 获取 Agent 列表

```http
GET /api/agents
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "agentCode": "fz_shoushan_stone",
      "name": "寿山石雕器灵",
      "avatar": "https://example.com/avatar.png",
      "roleType": "CULTURAL",
      "personality": "沉稳、博学",
      "status": 1
    }
  ],
  "timestamp": "2026-05-02T10:30:00"
}
```

完整字段见 [通用 Agent 结构](../common/response.md#agent)。

## 2. 获取 Agent 详情

```http
GET /api/agents/{code}
```

路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| code | string | Agent 编码，例如 `fz_shoushan_stone` |

成功响应：`data` 为单个 Agent。

未找到时：

```json
{
  "code": 404,
  "message": "Agent不存在",
  "timestamp": "2026-05-02T10:30:00"
}
```
