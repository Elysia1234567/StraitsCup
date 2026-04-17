package com.omnisource.service;

import com.omnisource.dto.request.LoginRequest;
import com.omnisource.dto.request.RefreshTokenRequest;
import com.omnisource.dto.request.RegisterRequest;
import com.omnisource.dto.response.LoginResponse;
import com.omnisource.dto.response.RegisterResponse;
import com.omnisource.dto.response.TokenResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册响应
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     *
     * @param token 访问令牌
     */
    void logout(String token);

    /**
     * 刷新Token
     *
     * @param request 刷新Token请求
     * @return Token响应
     */
    TokenResponse refreshToken(RefreshTokenRequest request);
}
