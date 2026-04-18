package com.omnisource.exception;

/**
 * 错误码接口
 * 所有错误码枚举都应该实现此接口
 *
 * @author OmniSource
 */
public interface ErrorCode {

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    Integer getCode();

    /**
     * 获取错误消息
     *
     * @return 错误消息
     */
    String getMessage();
}
