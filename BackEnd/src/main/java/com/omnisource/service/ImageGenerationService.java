package com.omnisource.service;

import com.omnisource.entity.ImageGenerationTask;

public interface ImageGenerationService {
    String submitTask(Long userId, Long roomId, String prompt, String style);
    String generateImageAndReturnUrl(Long userId, Long roomId, String prompt, String style);
    String generateImageAndReturnUrl(Long userId, Long roomId, String prompt, String style, String referenceImageUrl);
    ImageGenerationTask getTask(String taskId);
    void processTask(String taskId);
}
