package com.omnisource.exception;

/**
 * 用户已存在异常
 * 用于注册时用户名或邮箱已被占用的场景
 *
 * @author OmniSource
 */
public class UserAlreadyExistsException extends BusinessException {

    /**
     * 根据字段名和值构造异常
     *
     * @param field 字段名（如 "username", "email"）
     * @param value 字段值
     */
    public UserAlreadyExistsException(String field, String value) {
        super(CommonErrorCode.USER_ALREADY_EXISTS, field + "已存在: " + value);
    }

    /**
     * 简单构造函数 - 用户名已存在
     *
     * @param username 用户名
     */
    public UserAlreadyExistsException(String username) {
        this("用户名", username);
    }
}
