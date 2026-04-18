package com.omnisource.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 流式 AI 响应 WebSocket 处理器
 * TODO: 实现流式 AI 响应功能
 * - 接收用户消息
 * - 调用千问 API 获取流式响应
 * - 实时推送 AI 响应到前端
 * - 支持取消流式传输
 */
@Slf4j
@Component
public class StreamWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("流式 WebSocket 连接建立: sessionId={}", session.getId());
        // TODO: 初始化流式会话
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到流式请求: sessionId={}, message={}", session.getId(), payload);
        // TODO: 调用千问 API 获取流式响应
        // TODO: 实时推送响应块到前端
        // TODO: 发送结束标记
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
        log.info("流式 WebSocket 连接关闭: sessionId={}, status={}", session.getId(), closeStatus);
        // TODO: 清理流式资源
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("流式 WebSocket 传输错误: sessionId={}", session.getId(), exception);
        // TODO: 清理会话和资源
    }
}
