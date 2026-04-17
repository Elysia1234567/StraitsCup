package com.omnisource.service;

import com.omnisource.dto.response.UserInfoResponse;
import com.omnisource.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 根据ID获取用户
     *
     * @param id 用户ID
     * @return 用户实体
     */
    User getUserById(Long id);

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    User getUserByUsername(String username);

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息响应
     */
    UserInfoResponse getCurrentUserInfo();

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean isUsernameExists(String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean isEmailExists(String email);

    /**
     * 更新用户最后登录信息
     *
     * @param userId 用户ID
     * @param ip     IP地址
     */
    void updateLastLoginInfo(Long userId, String ip);
}
