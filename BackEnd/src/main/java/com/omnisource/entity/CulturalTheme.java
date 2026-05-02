package com.omnisource.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CulturalTheme {
    private Long id;
    private String themeCode;
    private String name;
    private String description;
    private String category;
    private String coverImage;
    private String region;
    private String era;
    private String knowledgeBase;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
