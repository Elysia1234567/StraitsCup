package com.omnisource.service.impl;

import com.omnisource.dto.request.LoginRequest;
import com.omnisource.dto.request.RefreshTokenRequest;
import com.omnisource.dto.request.RegisterRequest;
import com.omnisource.dto.response.LoginResponse;
import com.omnisource.dto.response.RegisterResponse;
import com.omnisource.dto.response.TokenResponse;
import com.omnisource.entity.User;
import com.omnisource.enums.UserRole;
import com.omnisource.enums.UserStatus;
import com.omnisource.exception.BusinessException;
import com.omnisource.mapper.UserMapper;
import com.omnisource.service.AuthService;
import com.omnisource.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String REFRESH_TOKEN_BLACKLIST_PREFIX = "refresh:blacklist:";
    private static final String ACCESS_TOKEN_BLACKLIST_PREFIX = "access:blacklist:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userMapper.countByEmail(request.getEmail()) > 0) {
                throw new BusinessException(400, "邮箱已被注册");
            }
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(UserStatus.ENABLED.getCode());
        user.setRole(UserRole.USER.getCode());
        user.setTokenVersion(0);
        user.setIsDeleted(0);

        // 保存用户
        userMapper.insert(user);

        // 生成Token
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getTokenVersion());

        log.info("用户注册成功: {}", user.getUsername());

        return RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != UserStatus.ENABLED.getCode()) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 生成Token
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getTokenVersion());

        log.info("用户登录成功: {}", user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    @Override
    public void logout(String token) {
        try {
            // 解析Token
            Claims claims = jwtUtil.parseToken(token);
            long expiration = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            long ttl = expiration - now;

            // 将Token加入黑名单
            if (ttl > 0) {
                String type = claims.get("type", String.class);
                if ("access".equals(type)) {
                    redisTemplate.opsForValue()
                            .set(ACCESS_TOKEN_BLACKLIST_PREFIX + token, "1", Duration.ofMillis(ttl));
                } else if ("refresh".equals(type)) {
                    redisTemplate.opsForValue()
                            .set(REFRESH_TOKEN_BLACKLIST_PREFIX + token, "1", Duration.ofMillis(ttl));
                }
            }

            log.info("用户登出成功");
        } catch (Exception e) {
            log.warn("登出时解析Token失败", e);
        }
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 验证Refresh Token是否有效
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "刷新令牌已过期或无效");
        }

        // 检查是否在黑名单中
        Boolean isBlacklisted = redisTemplate.hasKey(REFRESH_TOKEN_BLACKLIST_PREFIX + refreshToken);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new BusinessException(401, "刷新令牌已被注销");
        }

        // 获取Token中的用户信息
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        Integer tokenVersion = jwtUtil.getTokenVersionFromToken(refreshToken);

        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() != UserStatus.ENABLED.getCode()) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 验证Token版本（如果修改过密码，旧Token应该失效）
        if (!user.getTokenVersion().equals(tokenVersion)) {
            throw new BusinessException(401, "登录状态已过期，请重新登录");
        }

        // 将旧的Refresh Token加入黑名单
        long expiration = jwtUtil.getExpirationDateFromToken(refreshToken);
        long ttl = expiration - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue()
                    .set(REFRESH_TOKEN_BLACKLIST_PREFIX + refreshToken, "1", Duration.ofMillis(ttl));
        }

        // 生成新的Token
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getTokenVersion());

        log.info("Token刷新成功: {}", user.getUsername());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }
}
