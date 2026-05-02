package com.omnisource.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImageGenerationTask {
    private Long id;
    private String taskId;
    private Long userId;
    private Long roomId;
    private String prompt;
    private String style;
    private String status;
    private String resultUrl;
    private String errorMessage;
    private Integer progress;
    private String model;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
