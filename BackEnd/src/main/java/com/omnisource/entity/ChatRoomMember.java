package com.omnisource.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatRoomMember {
    private Long id;
    private Long roomId;
    private String memberType;
    private Long userId;
    private Long agentId;
    private String displayName;
    private String avatar;
    private String roleInRoom;
    private LocalDateTime lastSpeakTime;
    private Integer speakCount;
    private LocalDateTime joinTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
