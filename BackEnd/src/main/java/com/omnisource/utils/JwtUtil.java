package com.omnisource.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT密钥
     */
    @Value("${jwt.secret:OmniSourceSecretKey2026ForJWTGenerationAndValidation}")
    private String secret;

    /**
     * Access Token有效期（默认2小时）
     */
    @Value("${jwt.access-token-expiration:7200000}")
    private Long accessTokenExpiration;

    /**
     * Refresh Token有效期（默认7天）
     */
    @Value("${jwt.refresh-token-expiration:604800000}")
    private Long refreshTokenExpiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成Access Token
     *
     * @param userId       用户ID
     * @param username     用户名
     * @param role         角色
     * @param tokenVersion Token版本
     * @return Access Token
     */
    public String generateAccessToken(Long userId, String username, Integer role, Integer tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("tokenVersion", tokenVersion);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成Refresh Token
     *
     * @param userId       用户ID
     * @param tokenVersion Token版本
     * @return Refresh Token
     */
    public String generateRefreshToken(Long userId, Integer tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenVersion", tokenVersion);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析Token
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token格式");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误");
            return false;
        } catch (SignatureException e) {
            log.warn("Token签名验证失败");
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token为空或非法");
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中获取角色
     */
    public Integer getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", Integer.class);
    }

    /**
     * 从Token中获取Token版本
     */
    public Integer getTokenVersionFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("tokenVersion", Integer.class);
    }

    /**
     * 从Token中获取类型
     */
    public String getTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 获取Token过期时间（秒）
     */
    public Long getAccessTokenExpiration() {
        return accessTokenExpiration / 1000;
    }

    /**
     * 获取Token剩余有效时间
     */
    public Long getExpirationDateFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().getTime();
    }
}
