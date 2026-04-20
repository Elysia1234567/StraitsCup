package com.omnisource.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * 管理所有在线用户的 WebSocket 会话
 * 支持广播消息和单点发送
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 存储所有在线用户的会话
     * key: sessionId, value: WebSocketSession
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 存储用户ID和会话的映射
     * key: userId, value: sessionId
     * 用于通过用户ID查找会话
     */
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();

    /**
     * 用户连接时调用 - 添加会话
     */
    public void addSession(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);

        // 从 session 属性中获取用户ID（由拦截器设置）
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessionMap.put(userId, sessionId);
        }

        log.info("用户连接成功，当前在线人数: {}, sessionId: {}, userId: {}",
                sessions.size(), sessionId, userId);
    }

    /**
     * 用户断开时调用 - 移除会话
     */
    public void removeSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null) {
            String userId = getUserIdFromSession(session);
            if (userId != null) {
                userSessionMap.remove(userId);
            }
        }
        log.info("用户断开连接，当前在线人数: {}, sessionId: {}",
                sessions.size(), sessionId);
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcastMessage(String message) {
        log.debug("广播消息: {}, 目标人数: {}", message, sessions.size());

        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("发送消息失败, sessionId: {}", session.getId(), e);
                }
            }
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendMessageToUser(String userId, String message) {
        String sessionId = userSessionMap.get(userId);
        if (sessionId == null) {
            log.warn("用户不在线, userId: {}", userId);
            return;
        }

        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("消息已发送给用户: {}", userId);
            } catch (IOException e) {
                log.error("发送消息给用户失败, userId: {}", userId, e);
            }
        }
    }

    /**
     * 发送消息给指定会话
     */
    public void sendMessageToSession(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送消息失败, sessionId: {}", sessionId, e);
            }
        }
    }

    /**
     * 获取当前在线人数
     */
    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 从 session 属性中获取用户ID
     */
    private String getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : null;
    }

    /**
     * 从 session 属性中获取用户名
     */
    public String getUsernameFromSession(WebSocketSession session) {
        Object username = session.getAttributes().get("username");
        return username != null ? username.toString() : "匿名用户";
    }
}
