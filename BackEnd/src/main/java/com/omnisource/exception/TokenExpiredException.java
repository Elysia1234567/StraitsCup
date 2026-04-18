package com.omnisource.exception;

/**
 * Token已过期异常
 * 用于Token超过有效期的场景
 *
 * @author OmniSource
 */
public class TokenExpiredException extends BusinessException {

    /**
     * 默认构造函数
     */
    public TokenExpiredException() {
        super(CommonErrorCode.TOKEN_EXPIRED);
    }

    /**
     * 带自定义消息的构造函数
     *
     * @param message 自定义错误消息
     */
    public TokenExpiredException(String message) {
        super(CommonErrorCode.TOKEN_EXPIRED, message);
    }

    /**
     * 带异常原因的构造函数
     *
     * @param cause 异常原因
     */
    public TokenExpiredException(Throwable cause) {
        super(CommonErrorCode.TOKEN_EXPIRED, cause);
    }
}
