# 用户模块接口文档

> 用户信息管理相关接口

---

## 目录

1. [获取当前用户信息](#1-获取当前用户信息)

---

## 1. 获取当前用户信息

获取当前登录用户的详细信息。

**请求信息：**
- **URL**: `/api/user/me`
- **Method**: `GET`
- **认证**: 需要

**请求示例：**

```http
GET /api/user/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**成功响应：**

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "userId": 10001,
        "username": "zhangsan",
        "nickname": "张三",
        "email": "zhangsan@example.com",
        "avatar": null,
        "role": 0,
        "createTime": "2026-04-17 10:30:00"
    }
}
```

**错误响应：**

```json
// 未登录
{
    "code": 401,
    "message": "未登录或登录已过期"
}
```

---

## 数据模型

### UserStatus (用户状态)

| 值 | 含义 |
|---|------|
| 0 | 禁用 |
| 1 | 正常 |

### UserRole (用户角色)

| 值 | 含义 |
|---|------|
| 0 | 普通用户 |
| 1 | 管理员 |
