# 认证模块接口文档

> 用户认证相关接口：注册、登录、登出、刷新令牌

---

## 目录

1. [用户注册](#1-用户注册)
2. [用户登录](#2-用户登录)
3. [用户登出](#3-用户登出)
4. [刷新令牌](#4-刷新令牌)

---

## 1. 用户注册

注册新用户账号。

**请求信息：**
- **URL**: `/api/auth/register`
- **Method**: `POST`
- **Content-Type**: `application/json`

**请求参数：**

| 字段 | 类型 | 必填 | 说明 | 约束条件 |
|-----|------|-----|------|---------|
| username | string | 是 | 用户名 | 4-20位，字母/数字/下划线 |
| password | string | 是 | 密码 | 8-20位，必须包含字母和数字 |
| email | string | 否 | 邮箱 | 符合邮箱格式，全局唯一 |
| nickname | string | 否 | 昵称 | 2-32位，默认为用户名 |

**请求示例：**

```http
POST /api/auth/register
Content-Type: application/json

{
    "username": "zhangsan",
    "password": "Abc123456",
    "email": "zhangsan@example.com",
    "nickname": "张三"
}
```

**成功响应：**

```json
{
    "code": 200,
    "message": "注册成功",
    "data": {
        "userId": 10001,
        "username": "zhangsan",
        "nickname": "张三",
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "expiresIn": 7200
    }
}
```

**错误响应：**

```json
// 用户名已存在
{
    "code": 400,
    "message": "用户名已存在"
}

// 参数校验失败
{
    "code": 400,
    "message": "密码长度必须在8-20位之间"
}
```

---

## 2. 用户登录

使用用户名和密码登录。

**请求信息：**
- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Content-Type**: `application/json`

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**请求示例：**

```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "zhangsan",
    "password": "Abc123456"
}
```

**成功响应：**

```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "userId": 10001,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": null,
        "role": 0,
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "expiresIn": 7200
    }
}
```

**错误响应：**

```json
// 账号或密码错误
{
    "code": 401,
    "message": "用户名或密码错误"
}

// 账号被禁用
{
    "code": 403,
    "message": "账号已被禁用"
}
```

---

## 3. 用户登出

退出当前登录状态。

**请求信息：**
- **URL**: `/api/auth/logout`
- **Method**: `POST`
- **认证**: 需要

**请求示例：**

```http
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**成功响应：**

```json
{
    "code": 200,
    "message": "登出成功",
    "data": null
}
```

---

## 4. 刷新令牌

使用 Refresh Token 获取新的 Access Token。

**请求信息：**
- **URL**: `/api/auth/refresh`
- **Method**: `POST`
- **Content-Type**: `application/json`

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| refreshToken | string | 是 | 刷新令牌 |

**请求示例：**

```http
POST /api/auth/refresh
Content-Type: application/json

{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**成功响应：**

```json
{
    "code": 200,
    "message": "刷新成功",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "expiresIn": 7200
    }
}
```

**错误响应：**

```json
// Token已过期
{
    "code": 401,
    "message": "刷新令牌已过期或无效"
}

// Token已被注销
{
    "code": 401,
    "message": "刷新令牌已被注销"
}
```

---

## 数据模型

### Token说明

| 类型 | 有效期 | 用途 |
|-----|-------|-----|
| Access Token | 2小时 | API请求认证 |
| Refresh Token | 7天 | 刷新Access Token |
