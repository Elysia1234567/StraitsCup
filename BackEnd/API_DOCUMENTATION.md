# OmniSource 用户认证接口文档

> 版本: v1.0  
> 更新日期: 2026-04-17

---

## 目录

1. [接口概览](#接口概览)
2. [基础信息](#基础信息)
3. [接口详情](#接口详情)
4. [数据模型](#数据模型)
5. [错误码说明](#错误码说明)
6. [测试用例](#测试用例)

---

## 接口概览

| 接口名称 | 请求方式 | 路径 | 认证要求 |
|---------|---------|------|---------|
| 用户注册 | POST | `/api/auth/register` | 无需认证 |
| 用户登录 | POST | `/api/auth/login` | 无需认证 |
| 用户登出 | POST | `/api/auth/logout` | 需要认证 |
| 刷新令牌 | POST | `/api/auth/refresh` | 无需认证 |
| 获取当前用户 | GET | `/api/user/me` | 需要认证 |

---

## 基础信息

### Base URL

```
http://localhost:8080
```

### 认证方式

使用 **Bearer Token** 进行认证：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 统一响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": {}
}
```

---

## 接口详情

### 1. 用户注册

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

### 2. 用户登录

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

### 3. 用户登出

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

### 4. 刷新令牌

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

### 5. 获取当前用户信息

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

### Token说明

| 类型 | 有效期 | 用途 |
|-----|-------|-----|
| Access Token | 2小时 | API请求认证 |
| Refresh Token | 7天 | 刷新Access Token |

---

## 错误码说明

| 状态码 | 含义 | 说明 |
|-------|------|-----|
| 200 | 成功 | 请求处理成功 |
| 400 | 请求参数错误 | 参数校验失败或业务逻辑错误 |
| 401 | 未认证 | Token无效、过期或用户未登录 |
| 403 | 无权限 | 用户没有访问该资源的权限 |
| 404 | 资源不存在 | 请求的资源不存在 |
| 500 | 服务器错误 | 系统内部错误 |

---

## 测试用例

### 用例1: 正常注册

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456",
    "email": "testuser@example.com",
    "nickname": "测试用户"
  }'
```

**预期结果:**
- HTTP Status: 200
- 返回包含 userId、accessToken、refreshToken 的响应

---

### 用例2: 用户名已存在

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456",
    "email": "another@example.com"
  }'
```

**预期结果:**
- HTTP Status: 400
- 响应: `{"code":400,"message":"用户名已存在"}`

---

### 用例3: 密码格式错误

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "123",
    "email": "newuser@example.com"
  }'
```

**预期结果:**
- HTTP Status: 400
- 响应包含密码长度校验失败的提示

---

### 用例4: 正常登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456"
  }'
```

**预期结果:**
- HTTP Status: 200
- 返回包含 accessToken 的登录响应

---

### 用例5: 登录密码错误

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "WrongPassword"
  }'
```

**预期结果:**
- HTTP Status: 401
- 响应: `{"code":401,"message":"用户名或密码错误"}`

---

### 用例6: 获取用户信息（带Token）

```bash
curl -X GET http://localhost:8080/api/user/me \
  -H "Authorization: Bearer {access_token}"
```

**预期结果:**
- HTTP Status: 200
- 返回当前用户信息

---

### 用例7: 获取用户信息（无Token）

```bash
curl -X GET http://localhost:8080/api/user/me
```

**预期结果:**
- HTTP Status: 401
- 响应: `{"code":401,"message":"未登录或登录已过期"}`

---

### 用例8: 登出

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer {access_token}"
```

**预期结果:**
- HTTP Status: 200
- 响应: `{"code":200,"message":"登出成功"}`

---

### 用例9: 刷新Token

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "{refresh_token}"
  }'
```

**预期结果:**
- HTTP Status: 200
- 返回新的 accessToken 和 refreshToken

---

### 用例10: 使用已注销的Token

1. 先执行登出
2. 再用相同的Token访问 `/api/user/me`

```bash
curl -X GET http://localhost:8080/api/user/me \
  -H "Authorization: Bearer {已注销的token}"
```

**预期结果:**
- HTTP Status: 401
- 响应: `{"code":401,"message":"未登录或登录已过期"}`

---

## Postman集合

### 环境变量

| 变量名 | 初始值 | 说明 |
|-------|-------|-----|
| base_url | http://localhost:8080 | API基础URL |
| access_token | | Access Token |
| refresh_token | | Refresh Token |

### 预请求脚本（登录/注册后自动保存Token）

```javascript
// 在登录/注册接口的 Tests 标签页中添加
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.data && jsonData.data.accessToken) {
        pm.environment.set("access_token", jsonData.data.accessToken);
        pm.environment.set("refresh_token", jsonData.data.refreshToken);
    }
}
```

---

## 前端集成示例

### Axios拦截器配置

```javascript
import axios from 'axios';

// 创建实例
const api = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000
});

// 请求拦截器 - 添加Token
api.interceptors.request.use(
    config => {
        const token = localStorage.getItem('access_token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

// 响应拦截器 - 处理Token过期
api.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;
        
        // Token过期且未重试过
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            
            try {
                const refreshToken = localStorage.getItem('refresh_token');
                const response = await axios.post('/api/auth/refresh', {
                    refreshToken
                });
                
                const { accessToken, refreshToken: newRefreshToken } = response.data.data;
                localStorage.setItem('access_token', accessToken);
                localStorage.setItem('refresh_token', newRefreshToken);
                
                // 重试原请求
                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return api(originalRequest);
            } catch (refreshError) {
                // 刷新失败，跳转登录
                localStorage.removeItem('access_token');
                localStorage.removeItem('refresh_token');
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }
        
        return Promise.reject(error);
    }
);

export default api;
```

---

*文档结束*
