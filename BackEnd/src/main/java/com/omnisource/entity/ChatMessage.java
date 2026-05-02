package com.omnisource.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private Long roomId;
    private String messageType;
    private String senderType;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String imageUrl;
    private Long replyToMessageId;
    private String metadata;
    private Integer isStream;
    private String streamId;
    private Integer searchEnabled;
    private String searchResults;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
