package com.omnisource.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务异常基类
 * 支持错误码枚举和自定义错误消息
 *
 * @author OmniSource
 */
@Getter
@Setter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 构造函数 - 仅消息，使用默认错误码 500
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = CommonErrorCode.INTERNAL_ERROR.getCode();
        this.message = message;
    }

    /**
     * 构造函数 - 自定义错误码和消息
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数 - 使用错误码枚举
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息
     *
     * @param errorCode     错误码枚举
     * @param customMessage 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }

    /**
     * 构造函数 - 自定义错误码、消息和异常原因
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   异常原因
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数 - 使用错误码枚举 + 异常原因
     *
     * @param errorCode 错误码枚举
     * @param cause    异常原因
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息 + 异常原因
     *
     * @param errorCode     错误码枚举
     * @param customMessage 自定义错误消息
     * @param cause         异常原因
     */
    public BusinessException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }
}
