# OmniSource API 接口文档

> 版本: v1.0  
> 更新日期: 2026-04-18

## 目录结构

| 模块 | 路径 | 说明 |
|-----|------|-----|
| 认证模块 | [auth/authentication.md](auth/authentication.md) | 用户注册、登录、登出、刷新令牌 |
| 用户模块 | [user/user.md](user/user.md) | 用户信息管理 |
| RAG模块 | 内嵌于本文档 | Milvus建库、JSONL导入、向量检索、Prompt拼接 |

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

## 错误码说明

| 状态码 | 含义 | 说明 |
|-------|------|-----|
| 200 | 成功 | 请求处理成功 |
| 400 | 请求参数错误 | 参数校验失败或业务逻辑错误 |
| 401 | 未认证 | Token无效、过期或用户未登录 |
| 403 | 无权限 | 用户没有访问该资源的权限 |
| 404 | 资源不存在 | 请求的资源不存在 |
| 500 | 服务器错误 | 系统内部错误 |

## 接口概览

| 接口名称 | 请求方式 | 路径 | 认证要求 | 文档位置 |
|---------|---------|------|---------|---------|
| 用户注册 | POST | `/api/auth/register` | 无需认证 | [authentication.md](auth/authentication.md#1-用户注册) |
| 用户登录 | POST | `/api/auth/login` | 无需认证 | [authentication.md](auth/authentication.md#2-用户登录) |
| 用户登出 | POST | `/api/auth/logout` | 需要认证 | [authentication.md](auth/authentication.md#3-用户登出) |
| 刷新令牌 | POST | `/api/auth/refresh` | 无需认证 | [authentication.md](auth/authentication.md#4-刷新令牌) |
| 获取当前用户 | GET | `/api/user/me` | 需要认证 | [user.md](user/user.md#1-获取当前用户信息) |
| 重建RAG知识库 | POST | `/api/rag/reload` | 无需认证 | 本文档-RAG最小闭环 |
| 向量检索 | GET | `/api/rag/retrieve` | 无需认证 | 本文档-RAG最小闭环 |
| Prompt上下文预览 | GET | `/api/rag/prompt` | 无需认证 | 本文档-RAG最小闭环 |

## RAG最小闭环

### 1) 建 Milvus collection + 导入 JSONL

```http
POST /api/rag/reload
```

说明：接口会执行 `RagService.reload()`，包含创建/重建 collection、生成向量、写入 Milvus、建索引。

### 2) 向量检索

```http
GET /api/rag/retrieve?question=泉州木偶戏的特点&topK=3
```

### 3) 检索上下文拼接（用于 Prompt）

```http
GET /api/rag/prompt?question=泉州木偶戏的特点&topK=3
```

### 4) AIGC 问答（已自动拼接 RAG 上下文）

```http
GET /api/aigc/chat?message=泉州木偶戏的特点
```

```http
GET /api/aigc/stream?message=泉州木偶戏的特点
```
