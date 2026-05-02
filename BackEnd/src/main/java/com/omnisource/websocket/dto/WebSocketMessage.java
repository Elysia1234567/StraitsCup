package com.omnisource.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {

    private MessageType type;
    private SenderType senderType;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String imageUrl;
    private Long roomId;
    private String streamId;
    private Long messageId;
    private Long replyToMessageId;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    private Integer onlineCount;

    public enum MessageType {
        CHAT, AGENT_START, AGENT_CHUNK, AGENT_END,
        IMAGE, EMOJI, TYPING, PROGRESS, SYSTEM, ERROR
    }

    public enum SenderType {
        USER, AGENT, SYSTEM
    }
}
