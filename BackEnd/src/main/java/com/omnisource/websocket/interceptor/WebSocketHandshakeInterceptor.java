package com.omnisource.websocket.interceptor;

import com.omnisource.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 验证 JWT Token 并提取用户信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                    org.springframework.http.server.ServerHttpResponse response,
                                    org.springframework.web.socket.WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) throws Exception {

        // 从查询参数中获取 Token
        String query = request.getURI().getQuery();
        if (query == null || !query.contains("token=")) {
            log.warn("WebSocket 握手失败：缺少 Token");
            return false;
        }

        // 提取 Token 参数
        String token = extractToken(query);
        if (token == null || token.isBlank()) {
            log.warn("WebSocket 握手失败：Token 参数缺失或为空");
            return false;
        }

        try {
            // 验证 Token
            if (!jwtUtil.validateToken(token)) {
                log.warn("WebSocket 握手失败：Token 无效");
                return false;
            }

            // 提取用户信息
            String username = jwtUtil.getUsernameFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);

            // ✅ 添加空值检查
            if (username == null || userId == null) {
                log.warn("WebSocket 握手失败：Token 解析失败，用户信息为空");
                return false;
            }

            // 存储到 Session 属性中
            attributes.put("username", username);
            attributes.put("userId", userId);
            attributes.put("token", token);

            // ✅ 只记录 ID，不打印敏感的用户名
            log.info("WebSocket 握手成功：userId={}", userId);
            return true;

        } catch (Exception e) {
            log.error("WebSocket 握手失败：Token 解析错误", e);
            return false;
        }
    }

    /**
     * 从查询参数中提取 Token
     * @param query 查询参数字符串
     * @return Token 字符串，如果不存在返回 null
     */
    private String extractToken(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("token=")) {
                String token = param.substring(6);
                // URL 解码处理
                return URLDecoder.decode(token, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               org.springframework.web.socket.WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.error("WebSocket 握手后处理异常", exception);
        }
    }
}
