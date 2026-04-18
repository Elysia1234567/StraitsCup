package com.omnisource.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 聊聊 WebSocket 处理器
 * TODO: 实现多用户群聊功能
 * - 维护在线用户会话
 * - 广播消息到所有连接
 * - 处理用户加入/离开事件
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket 连接建立: sessionId={}", session.getId());
        // TODO: 将会话添加到在线用户列表
        // TODO: 广播用户加入消息
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到消息: sessionId={}, message={}", session.getId(), payload);
        // TODO: 转发消息到所有连接
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
        log.info("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), closeStatus);
        // TODO: 从在线用户列表移除会话
        // TODO: 广播用户离开消息
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
        // TODO: 清理会话资源
    }
}
