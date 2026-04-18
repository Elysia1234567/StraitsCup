package com.omnisource.exception;

/**
 * Token无效异常
 * 用于Token格式错误、签名错误等场景
 *
 * @author OmniSource
 */
public class TokenInvalidException extends BusinessException {

    /**
     * 默认构造函数
     */
    public TokenInvalidException() {
        super(CommonErrorCode.TOKEN_INVALID);
    }

    /**
     * 带自定义消息的构造函数
     *
     * @param message 自定义错误消息
     */
    public TokenInvalidException(String message) {
        super(CommonErrorCode.TOKEN_INVALID, message);
    }

    /**
     * 带异常原因的构造函数
     *
     * @param cause 异常原因
     */
    public TokenInvalidException(Throwable cause) {
        super(CommonErrorCode.TOKEN_INVALID, cause);
    }
}
