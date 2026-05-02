package com.omnisource.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatRoom {
    private Long id;
    private String roomCode;
    private Long userId;
    private Long themeId;
    private String name;
    private String description;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer messageCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
