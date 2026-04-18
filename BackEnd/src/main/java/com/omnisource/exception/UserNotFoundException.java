package com.omnisource.exception;

/**
 * 用户不存在异常
 *
 * @author OmniSource
 */
public class UserNotFoundException extends BusinessException {

    /**
     * 根据用户ID构造异常
     *
     * @param userId 用户ID
     */
    public UserNotFoundException(Long userId) {
        super(CommonErrorCode.USER_NOT_FOUND, "用户不存在: userId=" + userId);
    }

    /**
     * 根据用户名构造异常
     *
     * @param username 用户名
     */
    public UserNotFoundException(String username) {
        super(CommonErrorCode.USER_NOT_FOUND, "用户不存在: " + username);
    }
}
