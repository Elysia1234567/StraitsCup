package com.omnisource.exception;

import lombok.Getter;

/**
 * 通用错误码枚举。
 */
@Getter
public enum CommonErrorCode implements ErrorCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    REQUEST_TIMEOUT(408, "请求超时"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    INTERNAL_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    BUSINESS_ERROR(1000, "业务处理失败"),

    VALIDATION_ERROR(1301, "参数校验失败"),
    TYPE_MISMATCH_ERROR(1302, "参数类型错误"),
    MISSING_PARAMETER(1303, "缺少必需参数"),

    AI_SERVICE_ERROR(1401, "AI服务调用失败"),
    AI_RATE_LIMIT_ERROR(1402, "AI服务调用频率超限");

    private final Integer code;
    private final String message;

    CommonErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
