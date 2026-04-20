package com.omnisource.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.omnisource.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 群聊 WebSocket 处理器
 * 处理用户连接、消息收发、广播等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /**
     * 注入会话管理器
     */
    private final WebSocketSessionManager sessionManager;

    /**
     * JSON 序列化工具
     * 配置支持 JDK8 日期时间类型
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 连接建立后触发
     * - 注册会话
     * - 广播用户加入消息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String username = sessionManager.getUsernameFromSession(session);

        // 1. 注册到会话管理器
        sessionManager.addSession(sessionId, session);

        // 2. 广播系统消息：某某加入了聊天室
        int onlineCount = sessionManager.getOnlineCount();
        WebSocketMessage joinMessage = WebSocketMessage.system(
                username + " 加入了讨论",
                onlineCount
        );
        broadcastMessage(joinMessage);

        log.info("用户加入群聊: username={}, sessionId={}, 在线人数={}",
                username, sessionId, onlineCount);
    }

    /**
     * 收到文本消息时触发
     * - 解析消息
     * - 广播给所有在线用户
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String username = sessionManager.getUsernameFromSession(session);
        String userId = getUserIdFromSession(session);

        log.debug("收到用户消息: username={}, content={}", username, payload);

        try {
            // 构建聊天消息
            WebSocketMessage chatMessage = WebSocketMessage.chat(
                    userId,
                    username,
                    payload
            );

            // 广播给所有人（包括发送者自己）
            broadcastMessage(chatMessage);

        } catch (Exception e) {
            log.error("处理消息失败", e);
            // 发送错误消息给发送者
            WebSocketMessage errorMessage = WebSocketMessage.error("消息处理失败，请重试");
            sendMessageToSession(session.getId(), errorMessage);
        }
    }

    /**
     * 连接关闭后触发
     * - 移除会话
     * - 广播用户离开消息
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String username = sessionManager.getUsernameFromSession(session);

        // 1. 从会话管理器移除
        sessionManager.removeSession(sessionId);

        // 2. 广播系统消息：某某离开了
        int onlineCount = sessionManager.getOnlineCount();
        WebSocketMessage leaveMessage = WebSocketMessage.system(
                username + " 离开了讨论",
                onlineCount
        );
        broadcastMessage(leaveMessage);

        log.info("用户离开群聊: username={}, sessionId={}, 关闭状态={}, 在线人数={}",
                username, sessionId, status, onlineCount);
    }

    /**
     * 传输错误时触发
     * - 记录错误日志
     * - 尝试关闭会话
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    private void broadcastMessage(WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastMessage(json);
        } catch (Exception e) {
            log.error("广播消息序列化失败", e);
        }
    }

    /**
     * 发送消息给指定会话
     */
    private void sendMessageToSession(String sessionId, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sessionManager.sendMessageToSession(sessionId, json);
        } catch (Exception e) {
            log.error("发送消息序列化失败", e);
        }
    }

    /**
     * 从 session 属性中获取用户ID
     */
    private String getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : null;
    }
}
