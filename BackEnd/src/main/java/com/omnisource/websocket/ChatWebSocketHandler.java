package com.omnisource.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisource.service.AgentChatService;
import com.omnisource.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final AgentChatService agentChatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = extractRoomId(session);
        if (roomId == null) {
            log.warn("WebSocket连接缺少roomId参数");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        session.getAttributes().put("roomId", roomId);
        sessionManager.addSessionToRoom(roomId, session);

        int online = sessionManager.getRoomOnlineCount(roomId);
        WebSocketMessage joinMsg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.SYSTEM)
                .senderType(WebSocketMessage.SenderType.SYSTEM)
                .senderId("SYSTEM")
                .senderName("系统")
                .roomId(roomId)
                .content("用户加入了聊天室")
                .onlineCount(online)
                .timestamp(LocalDateTime.now())
                .build();
        sessionManager.broadcastToRoom(roomId, joinMsg);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long roomId = sessionManager.getRoomIdFromSession(session);
        Long userId = sessionManager.getUserIdFromSession(session);

        if (roomId == null) {
            log.warn("收到消息但session未绑定roomId");
            return;
        }

        try {
            WebSocketMessage wsMsg = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);

            switch (wsMsg.getType()) {
                case CHAT, IMAGE, EMOJI -> {
                    String content = wsMsg.getContent() != null ? wsMsg.getContent() : "";
                    String imageUrl = wsMsg.getImageUrl();
                    boolean searchEnabled = wsMsg.getMetadata() != null
                            && Boolean.TRUE.equals(wsMsg.getMetadata().get("searchEnabled"));
                    boolean ragEnabled = wsMsg.getMetadata() != null
                            && Boolean.TRUE.equals(wsMsg.getMetadata().get("ragEnabled"));
                    agentChatService.handleUserMessage(roomId, userId, content, imageUrl, searchEnabled, ragEnabled);
                }
                default -> log.debug("未处理的消息类型: {}", wsMsg.getType());
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            WebSocketMessage errorMsg = WebSocketMessage.builder()
                    .type(WebSocketMessage.MessageType.ERROR)
                    .senderType(WebSocketMessage.SenderType.SYSTEM)
                    .roomId(roomId)
                    .content("消息处理失败: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            sessionManager.sendToSession(session.getId(), errorMsg);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long roomId = sessionManager.getRoomIdFromSession(session);
        if (roomId != null) {
            sessionManager.removeSessionFromRoom(roomId, session);
            int online = sessionManager.getRoomOnlineCount(roomId);
            WebSocketMessage leaveMsg = WebSocketMessage.builder()
                    .type(WebSocketMessage.MessageType.SYSTEM)
                    .senderType(WebSocketMessage.SenderType.SYSTEM)
                    .senderId("SYSTEM")
                    .senderName("系统")
                    .roomId(roomId)
                    .content("用户离开了聊天室")
                    .onlineCount(online)
                    .timestamp(LocalDateTime.now())
                    .build();
            sessionManager.broadcastToRoom(roomId, leaveMsg);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);
        Long roomId = sessionManager.getRoomIdFromSession(session);
        if (roomId != null) {
            sessionManager.removeSessionFromRoom(roomId, session);
        }
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Long extractRoomId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("roomId=")) {
                try {
                    return Long.valueOf(param.substring(7));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
