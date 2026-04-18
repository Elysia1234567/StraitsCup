package com.omnisource.exception;

/**
 * 账号已被禁用异常
 *
 * @author OmniSource
 */
public class AccountDisabledException extends BusinessException {

    /**
     * 默认构造函数
     */
    public AccountDisabledException() {
        super(CommonErrorCode.ACCOUNT_DISABLED);
    }

    /**
     * 带用户名的构造函数
     *
     * @param username 用户名
     */
    public AccountDisabledException(String username) {
        super(CommonErrorCode.ACCOUNT_DISABLED, "账号已被禁用: " + username);
    }
}
