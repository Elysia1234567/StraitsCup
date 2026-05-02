package com.omnisource.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                    org.springframework.http.server.ServerHttpResponse response,
                                    org.springframework.web.socket.WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) throws Exception {

        String query = request.getURI().getQuery();

        Long roomId = extractLongParam(query, "roomId");
        if (roomId != null) {
            attributes.put("roomId", roomId);
        }

        attributes.put("username", "匿名用户");
        attributes.put("userId", 1L);
        log.info("WebSocket 匿名握手成功：roomId={}", roomId);
        return true;
    }

    private String extractStringParam(String query, String key) {
        if (query == null || !query.contains(key + "=")) {
            return null;
        }
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith(key + "=")) {
                String value = param.substring(key.length() + 1);
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private Long extractLongParam(String query, String key) {
        String value = extractStringParam(query, key);
        if (value == null) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
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
