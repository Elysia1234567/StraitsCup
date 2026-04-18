package com.omnisource.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一响应封装类
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    // ==================== HTTP 状态码常量 ====================

    /**
     * 成功 - 200
     */
    public static final int SUCCESS = 200;

    /**
     * 创建成功 - 201
     */
    public static final int CREATED = 201;

    /**
     * 无内容 - 204
     */
    public static final int NO_CONTENT = 204;

    /**
     * 错误请求 - 400
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权 - 401
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 禁止访问 - 403
     */
    public static final int FORBIDDEN = 403;

    /**
     * 未找到 - 404
     */
    public static final int NOT_FOUND = 404;

    /**
     * 内部服务器错误 - 500
     */
    public static final int INTERNAL_SERVER_ERROR = 500;

    // ==================== 字段 ====================

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ==================== 便捷静态方法 ====================

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(SUCCESS)
                .message("success")
                .build();
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(SUCCESS)
                .message("success")
                .data(data)
                .build();
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(SUCCESS)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 创建成功响应（201）
     */
    public static <T> Result<T> created(T data) {
        return Result.<T>builder()
                .code(CREATED)
                .message("created")
                .data(data)
                .build();
    }

    /**
     * 无内容响应（204）
     */
    public static <T> Result<T> noContent() {
        return Result.<T>builder()
                .code(NO_CONTENT)
                .message("no content")
                .build();
    }

    /**
     * 错误请求响应（400）
     */
    public static <T> Result<T> badRequest(String message) {
        return Result.<T>builder()
                .code(BAD_REQUEST)
                .message(message)
                .build();
    }

    /**
     * 未授权响应（401）
     */
    public static <T> Result<T> unauthorized(String message) {
        return Result.<T>builder()
                .code(UNAUTHORIZED)
                .message(message)
                .build();
    }

    /**
     * 禁止访问响应（403）
     */
    public static <T> Result<T> forbidden(String message) {
        return Result.<T>builder()
                .code(FORBIDDEN)
                .message(message)
                .build();
    }

    /**
     * 未找到响应（404）
     */
    public static <T> Result<T> notFound(String message) {
        return Result.<T>builder()
                .code(NOT_FOUND)
                .message(message)
                .build();
    }

    /**
     * 内部服务器错误响应（500）
     */
    public static <T> Result<T> serverError(String message) {
        return Result.<T>builder()
                .code(INTERNAL_SERVER_ERROR)
                .message(message)
                .build();
    }

    /**
     * 通用错误响应（带状态码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    // ==================== 链式调用方法 ====================

    /**
     * 设置状态码
     */
    public Result<T> setCode(Integer code) {
        this.code = code;
        return this;
    }

    /**
     * 设置消息
     */
    public Result<T> setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * 设置数据
     */
    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }
}
