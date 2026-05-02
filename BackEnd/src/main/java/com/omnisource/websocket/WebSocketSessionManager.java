package com.omnisource.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisource.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final ObjectMapper objectMapper;

    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();

    public void addSessionToRoom(Long roomId, WebSocketSession session) {
        allSessions.put(session.getId(), session);
        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("Session {} 加入聊天室 {}, 当前在线 {}", session.getId(), roomId, getRoomOnlineCount(roomId));
    }

    public void removeSessionFromRoom(Long roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
        allSessions.remove(session.getId());
        log.info("Session {} 离开聊天室 {}, 当前在线 {}", session.getId(), roomId, getRoomOnlineCount(roomId));
    }

    public void broadcastToRoom(Long roomId, WebSocketMessage message) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        log.error("广播消息失败, sessionId={}", session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("消息序列化失败", e);
        }
    }

    public void sendToSession(String sessionId, WebSocketMessage message) {
        WebSocketSession session = allSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("发送消息失败, sessionId={}", sessionId, e);
            }
        }
    }

    public int getRoomOnlineCount(Long roomId) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        return sessions == null ? 0 : sessions.size();
    }

    public String getUsernameFromSession(WebSocketSession session) {
        Object username = session.getAttributes().get("username");
        return username != null ? username.toString() : "匿名用户";
    }

    public Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    public Long getRoomIdFromSession(WebSocketSession session) {
        Object roomId = session.getAttributes().get("roomId");
        return roomId != null ? Long.valueOf(roomId.toString()) : null;
    }
}
