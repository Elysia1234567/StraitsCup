package com.omnisource.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 消息对象
 * 定义前后端通信的消息格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 发送者（用户ID或Agent名称）
     */
    private String sender;

    /**
     * 发送者昵称（用于显示）
     */
    private String senderNickname;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息发送时间
     */
    private LocalDateTime timestamp;

    /**
     * 在线人数（用于系统消息）
     */
    private Integer onlineCount;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 用户聊天消息
         */
        CHAT,

        /**
         * 系统通知（用户加入/离开等）
         */
        SYSTEM,

        /**
         * AI角色回复
         */
        AGENT,

        /**
         * 错误消息
         */
        ERROR
    }

    /**
     * 创建用户聊天消息
     */
    public static WebSocketMessage chat(String sender, String senderNickname, String content) {
        return WebSocketMessage.builder()
                .type(MessageType.CHAT)
                .sender(sender)
                .senderNickname(senderNickname)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建系统通知消息
     */
    public static WebSocketMessage system(String content, int onlineCount) {
        return WebSocketMessage.builder()
                .type(MessageType.SYSTEM)
                .sender("SYSTEM")
                .senderNickname("系统")
                .content(content)
                .onlineCount(onlineCount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建AI角色回复消息
     */
    public static WebSocketMessage agent(String agentName, String agentNickname, String content) {
        return WebSocketMessage.builder()
                .type(MessageType.AGENT)
                .sender(agentName)
                .senderNickname(agentNickname)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建错误消息
     */
    public static WebSocketMessage error(String errorMessage) {
        return WebSocketMessage.builder()
                .type(MessageType.ERROR)
                .sender("SYSTEM")
                .senderNickname("系统")
                .content(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
