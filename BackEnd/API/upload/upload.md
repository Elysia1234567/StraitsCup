# 上传接口

Base Path：`/api/upload`

## 1. 上传图片

```http
POST /api/upload/image
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | 是 | 待上传图片 |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "url": "https://example.com/upload/image.png",
    "filename": "image.png"
  },
  "timestamp": "2026-05-02T10:30:00"
}
```

curl 示例：

```bash
curl -X POST "http://localhost:8081/api/upload/image" \
  -F "file=@./image.png"
```
