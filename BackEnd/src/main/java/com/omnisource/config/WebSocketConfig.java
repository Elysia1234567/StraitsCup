package com.omnisource.config;

import com.omnisource.websocket.ChatWebSocketHandler;
import com.omnisource.websocket.StreamWebSocketHandler;
import com.omnisource.websocket.interceptor.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 * 配置 WebSocket 端点和消息处理器
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final StreamWebSocketHandler streamWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 群聊端点
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("https://your-frontend-domain.com"); // TODO: 需要前端提供具体域名或地址

        // 流式 AI 响应端点
        registry.addHandler(streamWebSocketHandler, "/ws/stream")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("https://your-frontend-domain.com"); // TODO: 需要前端提供具体域名或地址
    }
}
