package com.omnisource.exception;

import lombok.Getter;

/**
 * 通用错误码枚举
 * 错误码规范：
 * - 200: 成功
 * - 4xx: 客户端错误
 * - 5xx: 服务器错误
 * - 1xxx: 业务错误
 * - 11xx: 用户相关错误
 * - 12xx: 认证相关错误
 * - 13xx: 参数校验错误
 * - 14xx: AI服务相关错误
 *
 * @author OmniSource
 */
@Getter
public enum CommonErrorCode implements ErrorCode {

    // ==================== 成功 ====================
    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    // ==================== 客户端错误 4xx ====================
    /**
     * 请求参数错误
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 未认证或认证失败
     */
    UNAUTHORIZED(401, "未认证或认证失败"),

    /**
     * 无权限访问
     */
    FORBIDDEN(403, "无权限访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    /**
     * 资源冲突
     */
    CONFLICT(409, "资源冲突"),

    /**
     * 请求过于频繁
     */
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // ==================== 服务器错误 5xx ====================
    /**
     * 系统内部错误
     */
    INTERNAL_ERROR(500, "系统内部错误"),

    /**
     * 服务暂不可用
     */
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // ==================== 业务错误 1xxx ====================
    /**
     * 业务处理失败
     */
    BUSINESS_ERROR(1000, "业务处理失败"),

    // ==================== 用户相关 11xx ====================
    /**
     * 用户不存在
     */
    USER_NOT_FOUND(1101, "用户不存在"),

    /**
     * 用户名已存在
     */
    USERNAME_EXISTS(1102, "用户名已存在"),

    /**
     * 邮箱已被注册
     */
    EMAIL_EXISTS(1103, "邮箱已被注册"),

    /**
     * 密码错误
     */
    PASSWORD_ERROR(1104, "密码错误"),

    /**
     * 账号已被禁用
     */
    ACCOUNT_DISABLED(1105, "账号已被禁用"),

    /**
     * 用户已存在
     */
    USER_ALREADY_EXISTS(1106, "用户已存在"),

    // ==================== 认证相关 12xx ====================
    /**
     * Token无效
     */
    TOKEN_INVALID(1201, "Token无效"),

    /**
     * Token已过期
     */
    TOKEN_EXPIRED(1202, "Token已过期"),

    /**
     * Token已被注销
     */
    TOKEN_REVOKED(1203, "Token已被注销"),

    /**
     * 登录状态已过期
     */
    LOGIN_STATE_EXPIRED(1204, "登录状态已过期，请重新登录"),

    // ==================== 参数校验 13xx ====================
    /**
     * 参数校验失败
     */
    VALIDATION_ERROR(1301, "参数校验失败"),

    /**
     * 参数类型错误
     */
    TYPE_MISMATCH_ERROR(1302, "参数类型错误"),

    /**
     * 缺少必需参数
     */
    MISSING_PARAMETER(1303, "缺少必需参数"),

    // ==================== AI相关 14xx ====================
    /**
     * AI服务调用失败
     */
    AI_SERVICE_ERROR(1401, "AI服务调用失败"),

    /**
     * AI服务调用频率超限
     */
    AI_RATE_LIMIT_ERROR(1402, "AI服务调用频率超限");

    private final Integer code;
    private final String message;

    CommonErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
