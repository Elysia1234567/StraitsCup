# OmniSource 用户认证接口测试用例

> 详细的测试数据用例，用于接口测试和开发调试

---

## 目录

1. [测试数据准备](#测试数据准备)
2. [注册接口测试](#注册接口测试)
3. [登录接口测试](#登录接口测试)
4. [Token相关测试](#token相关测试)
5. [用户信息接口测试](#用户信息接口测试)

---

## 测试数据准备

### 预置用户数据（用于登录测试）

在数据库中预插入以下测试用户：

```sql
-- 测试用户1: 正常用户
INSERT INTO user (username, password, email, nickname, status, role, token_version, is_deleted)
VALUES ('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 'test@test.com', '测试用户', 1, 0, 0, 0);
-- 密码明文: Test123456

-- 测试用户2: 被禁用的用户
INSERT INTO user (username, password, email, nickname, status, role, token_version, is_deleted)
VALUES ('banneduser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 'banned@test.com', '禁用用户', 0, 0, 0, 0);
-- 密码明文: Test123456
```

---

## 注册接口测试

### TC-REG-001: 正常注册（完整参数）

**请求:**
```json
{
    "username": "newuser001",
    "password": "NewPass123",
    "email": "newuser001@test.com",
    "nickname": "新用户001"
}
```

**预期响应:**
```json
{
    "code": 200,
    "message": "注册成功",
    "data": {
        "userId": 10003,
        "username": "newuser001",
        "nickname": "新用户001",
        "accessToken": "eyJhbGci...",
        "refreshToken": "eyJhbGci...",
        "expiresIn": 7200
    }
}
```

---

### TC-REG-002: 正常注册（最小参数）

**请求:**
```json
{
    "username": "minuser",
    "password": "MinPass123"
}
```

**预期响应:**
```json
{
    "code": 200,
    "message": "注册成功",
    "data": {
        "userId": 10004,
        "username": "minuser",
        "nickname": "minuser",
        "accessToken": "eyJhbGci...",
        "refreshToken": "eyJhbGci...",
        "expiresIn": 7200
    }
}
```

---

### TC-REG-003: 用户名已存在

**请求:**
```json
{
    "username": "testuser",
    "password": "Test123456",
    "email": "unique@test.com"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "用户名已存在"
}
```

---

### TC-REG-004: 邮箱已存在

**请求:**
```json
{
    "username": "uniqueuser",
    "password": "Test123456",
    "email": "test@test.com"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "邮箱已被注册"
}
```

---

### TC-REG-005: 用户名过短（3位）

**请求:**
```json
{
    "username": "abc",
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "用户名长度必须在4-20位之间"
}
```

---

### TC-REG-006: 用户名过长（21位）

**请求:**
```json
{
    "username": "thisisaverylongusername1",
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "用户名长度必须在4-20位之间"
}
```

---

### TC-REG-007: 用户名包含非法字符

**请求:**
```json
{
    "username": "test@user!",
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "用户名只能包含字母、数字和下划线"
}
```

---

### TC-REG-008: 密码过短（7位）

**请求:**
```json
{
    "username": "validuser",
    "password": "Short1!"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "密码长度必须在8-20位之间"
}
```

---

### TC-REG-009: 密码无字母

**请求:**
```json
{
    "username": "validuser",
    "password": "123456789"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "密码必须包含字母和数字"
}
```

---

### TC-REG-010: 密码无数字

**请求:**
```json
{
    "username": "validuser",
    "password": "PasswordOnly"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "密码必须包含字母和数字"
}
```

---

### TC-REG-011: 邮箱格式错误

**请求:**
```json
{
    "username": "validuser",
    "password": "Test123456",
    "email": "invalid-email-format"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "邮箱格式不正确"
}
```

---

### TC-REG-012: 缺少用户名

**请求:**
```json
{
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "用户名不能为空"
}
```

---

### TC-REG-013: 缺少密码

**请求:**
```json
{
    "username": "validuser"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "密码不能为空"
}
```

---

### TC-REG-014: 空用户名

**请求:**
```json
{
    "username": "",
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "用户名不能为空"
}
```

---

### TC-REG-015: 空密码

**请求:**
```json
{
    "username": "validuser",
    "password": ""
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "密码不能为空"
}
```

---

## 登录接口测试

### TC-LOGIN-001: 正常登录

**请求:**
```json
{
    "username": "testuser",
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "userId": 10001,
        "username": "testuser",
        "nickname": "测试用户",
        "avatar": null,
        "role": 0,
        "accessToken": "eyJhbGci...",
        "refreshToken": "eyJhbGci...",
        "expiresIn": 7200
    }
}
```

---

### TC-LOGIN-002: 用户名错误

**请求:**
```json
{
    "username": "nonexistent",
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 401,
    "message": "用户名或密码错误"
}
```

---

### TC-LOGIN-003: 密码错误

**请求:**
```json
{
    "username": "testuser",
    "password": "WrongPass123"
}
```

**预期响应:**
```json
{
    "code": 401,
    "message": "用户名或密码错误"
}
```

---

### TC-LOGIN-004: 账号被禁用

**请求:**
```json
{
    "username": "banneduser",
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 403,
    "message": "账号已被禁用"
}
```

---

### TC-LOGIN-005: 缺少用户名

**请求:**
```json
{
    "password": "Test123456"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "用户名不能为空"
}
```

---

### TC-LOGIN-006: 缺少密码

**请求:**
```json
{
    "username": "testuser"
}
```

**预期响应:**
```json
{
    "code": 400,
    "message": "密码不能为空"
}
```

---

## Token相关测试

### TC-TOKEN-001: 刷新Token成功

**前置条件:** 使用有效的Refresh Token

**请求:**
```json
{
    "refreshToken": "{有效的refresh_token}"
}
```

**预期响应:**
```json
{
    "code": 200,
    "message": "刷新成功",
    "data": {
        "accessToken": "eyJhbGci...",
        "refreshToken": "eyJhbGci...",
        "expiresIn": 7200
    }
}
```

---

### TC-TOKEN-002: 刷新Token格式错误

**请求:**
```json
{
    "refreshToken": "invalid-token-format"
}
```

**预期响应:**
```json
{
    "code": 401,
    "message": "刷新令牌已过期或无效"
}
```

---

### TC-TOKEN-003: 刷新Token已过期

**前置条件:** 使用过期的Refresh Token

**请求:**
```json
{
    "refreshToken": "{过期的refresh_token}"
}
```

**预期响应:**
```json
{
    "code": 401,
    "message": "刷新令牌已过期或无效"
}
```

---

### TC-TOKEN-004: 登出成功

**前置条件:** 已登录，有有效的Access Token

**请求:**
```http
POST /api/auth/logout
Authorization: Bearer {access_token}
```

**预期响应:**
```json
{
    "code": 200,
    "message": "登出成功",
    "data": null
}
```

---

### TC-TOKEN-005: 使用已登出的Token

**前置条件:** Token已被登出

**请求:**
```http
GET /api/user/me
Authorization: Bearer {已登出的token}
```

**预期响应:**
```json
{
    "code": 401,
    "message": "未登录或登录已过期"
}
```

---

## 用户信息接口测试

### TC-USER-001: 获取当前用户信息（已登录）

**前置条件:** 已登录，有有效的Access Token

**请求:**
```http
GET /api/user/me
Authorization: Bearer {access_token}
```

**预期响应:**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "userId": 10001,
        "username": "testuser",
        "nickname": "测试用户",
        "email": "test@test.com",
        "avatar": null,
        "role": 0,
        "createTime": "2026-04-17 10:30:00"
    }
}
```

---

### TC-USER-002: 获取当前用户信息（未登录）

**请求:**
```http
GET /api/user/me
```

**预期响应:**
```json
{
    "code": 401,
    "message": "未登录或登录已过期"
}
```

---

### TC-USER-003: 获取当前用户信息（Token无效）

**请求:**
```http
GET /api/user/me
Authorization: Bearer invalid.token.here
```

**预期响应:**
```json
{
    "code": 401,
    "message": "未登录或登录已过期"
}
```

---

### TC-USER-004: 获取当前用户信息（Token过期）

**前置条件:** 使用已过期的Access Token

**请求:**
```http
GET /api/user/me
Authorization: Bearer {过期的token}
```

**预期响应:**
```json
{
    "code": 401,
    "message": "未登录或登录已过期"
}
```

---

### TC-USER-005: 获取当前用户信息（Token格式错误）

**请求:**
```http
GET /api/user/me
Authorization: malformatted-token
```

**预期响应:**
```json
{
    "code": 401,
    "message": "未登录或登录已过期"
}
```

---

### TC-USER-006: 获取当前用户信息（缺少Bearer前缀）

**请求:**
```http
GET /api/user/me
Authorization: {access_token}
```

**预期响应:**
```json
{
    "code": 401,
    "message": "未登录或登录已过期"
}
```

---

## 边界值测试

### 用户名边界值

| 测试项 | 输入值 | 预期结果 |
|-------|-------|---------|
| 最小长度-1 | "abc" (3位) | 失败，提示长度不足 |
| 最小长度 | "abcd" (4位) | 成功 |
| 最大长度 | "a".repeat(20) | 成功 |
| 最大长度+1 | "a".repeat(21) | 失败，提示长度超限 |

### 密码边界值

| 测试项 | 输入值 | 预期结果 |
|-------|-------|---------|
| 最小长度-1 | "Pass1!" (7位) | 失败，提示长度不足 |
| 最小长度 | "Pass1234" (8位) | 成功 |
| 最大长度 | "A1".repeat(10) (20位) | 成功 |
| 最大长度+1 | "A1".repeat(11) (21位) | 失败，提示长度超限 |

---

## 并发测试

### TC-CONCURRENT-001: 并发注册相同用户名

**场景:** 同时发送10个注册请求，用户名相同

**请求:**
```json
{
    "username": "concurrentuser",
    "password": "Test123456"
}
```

**预期结果:** 只有一个请求成功，其他返回"用户名已存在"

---

### TC-CONCURRENT-002: 并发刷新Token

**场景:** 使用同一个Refresh Token同时发送多个刷新请求

**预期结果:** 只有一个请求成功返回新Token，其他返回"刷新令牌已被注销"

---

## 安全测试

### TC-SECURITY-001: SQL注入尝试

**请求:**
```json
{
    "username": "' OR '1'='1",
    "password": "Test123456"
}
```

**预期响应:** 400 或 401（不会被SQL注入攻击）

---

### TC-SECURITY-002: XSS尝试

**请求:**
```json
{
    "username": "<script>alert('xss')</script>",
    "password": "Test123456"
}
```

**预期响应:** 400（用户名格式校验失败）

---

### TC-SECURITY-003: 超长请求体

**请求:** 发送超过1MB的请求体

**预期响应:** 413 Payload Too Large

---

*文档结束*
