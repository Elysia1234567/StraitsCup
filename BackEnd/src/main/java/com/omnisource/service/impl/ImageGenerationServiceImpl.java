package com.omnisource.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisource.entity.ImageGenerationTask;
import com.omnisource.exception.BusinessException;
import com.omnisource.exception.CommonErrorCode;
import com.omnisource.mapper.ImageGenerationTaskMapper;
import com.omnisource.service.ImageGenerationService;
import com.omnisource.websocket.WebSocketSessionManager;
import com.omnisource.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private static final String TASK_CACHE_PREFIX = "aigc:image:task:";
    private static final long TASK_CACHE_TTL_HOURS = 24;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final ImageGenerationTaskMapper taskMapper;
    private final WebSocketSessionManager sessionManager;
    private final StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Object imageGenerationLock = new Object();

    @Value("${aliyun.oss.agent-image-folder:OmniSource/chatroom/agents/}")
    private String agentImageFolder;

    @Value("${qianwen.image-model:qwen-image-2.0-pro}")
    private String imageModel;

    @Value("${qianwen.image-api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation}")
    private String imageApiUrl;

    @Value("${qianwen.image-size:1024*1024}")
    private String imageSize;

    @Value("${qianwen.api-key}")
    private String qianwenApiKey;

    @Override
    public String submitTask(Long userId, Long roomId, String prompt, String style) {
        String taskId = UUID.randomUUID().toString();

        ImageGenerationTask task = new ImageGenerationTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setRoomId(roomId);
        task.setPrompt(buildStyledPrompt(prompt, style));
        task.setStyle(style);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setModel(imageModel);

        taskMapper.insert(task);
        cacheTaskStatus(taskId, "PENDING", 0, null, null);
        return taskId;
    }

    @Override
    public String generateImageAndReturnUrl(Long userId, Long roomId, String prompt, String style) {
        return generateImageAndReturnUrl(userId, roomId, prompt, style, null);
    }

    @Override
    public String generateImageAndReturnUrl(Long userId, Long roomId, String prompt, String style, String referenceImageUrl) {
        String taskId = submitTask(userId, roomId, prompt, style);
        ImageGenerationTask task = taskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new BusinessException(CommonErrorCode.AI_SERVICE_ERROR,
                    "Image generation task not found: " + taskId);
        }

        try {
            updateTaskStatus(taskId, "PROCESSING", 20, null, null);
            String imageUrl;
            synchronized (imageGenerationLock) {
                imageUrl = generateImage(task.getPrompt(), referenceImageUrl);
            }
            log.info("Agent generated image target OSS folder: {}", agentImageFolder);
            updateTaskStatus(taskId, "SUCCESS", 100, imageUrl, null);
            return imageUrl;
        } catch (Exception e) {
            log.error("文生图任务处理失败: taskId={}", taskId, e);
            String errorMessage = shortenErrorMessage(e);
            updateTaskStatus(taskId, "FAILED", 0, null, errorMessage);
            throw new BusinessException(CommonErrorCode.AI_SERVICE_ERROR, errorMessage, e);
        }
    }

    @Override
    public ImageGenerationTask getTask(String taskId) {
        ImageGenerationTask cached = getCachedTask(taskId);
        if (cached != null) {
            ImageGenerationTask persisted = taskMapper.selectByTaskId(taskId);
            if (persisted != null) {
                cached.setId(persisted.getId());
                cached.setUserId(persisted.getUserId());
                cached.setRoomId(persisted.getRoomId());
                cached.setPrompt(persisted.getPrompt());
                cached.setStyle(persisted.getStyle());
                cached.setModel(persisted.getModel());
                cached.setCreateTime(persisted.getCreateTime());
                cached.setUpdateTime(persisted.getUpdateTime());
            }
            return cached;
        }
        return taskMapper.selectByTaskId(taskId);
    }

    @Override
    @Async
    public void processTask(String taskId) {
        ImageGenerationTask task = taskMapper.selectByTaskId(taskId);
        if (task == null) {
            return;
        }

        try {
            updateTaskStatus(taskId, "PROCESSING", 20, null, null);
            broadcastProgress(task.getRoomId(), taskId, 20);

            String imageUrl;
            synchronized (imageGenerationLock) {
                imageUrl = generateImage(task.getPrompt(), null);
            }
            log.info("Agent generated image target OSS folder: {}", agentImageFolder);

            updateTaskStatus(taskId, "SUCCESS", 100, imageUrl, null);
            broadcastProgress(task.getRoomId(), taskId, 100);
            broadcastImageResult(task.getRoomId(), taskId, imageUrl);
        } catch (Exception e) {
            log.error("文生图任务处理失败: taskId={}", taskId, e);
            String errorMessage = shortenErrorMessage(e);
            updateTaskStatus(taskId, "FAILED", 0, null, errorMessage);
            broadcastError(task.getRoomId(), taskId, errorMessage);
        }
    }

    private String generateImage(String prompt, String referenceImageUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(qianwenApiKey);

        List<Map<String, Object>> content = StringUtils.hasText(referenceImageUrl)
                ? List.of(
                        Map.of("image", referenceImageUrl),
                        Map.of("text", prompt)
                )
                : List.of(Map.of("text", prompt));

        Map<String, Object> body = Map.of(
                "model", imageModel,
                "input", Map.of(
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", content
                                )
                        )
                ),
                "parameters", Map.of("size", imageSize)
        );

        String responseBody = restTemplate.postForObject(imageApiUrl, new HttpEntity<>(body, headers), String.class);
        return extractImageUrl(responseBody);
    }

    private String extractImageUrl(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException(imageModel + " returned empty response");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("output").path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (content.isArray()) {
                    for (JsonNode item : content) {
                        JsonNode image = item.path("image");
                        if (image.isTextual()) {
                            return image.asText();
                        }
                        JsonNode url = item.path("url");
                        if (url.isTextual()) {
                            return url.asText();
                        }
                    }
                }
            }

            JsonNode directUrl = root.path("output").path("url");
            if (directUrl.isTextual()) {
                return directUrl.asText();
            }

            JsonNode results = root.path("output").path("results");
            if (results.isArray() && !results.isEmpty()) {
                JsonNode url = results.get(0).path("url");
                if (url.isTextual()) {
                    return url.asText();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + imageModel + " response: " + e.getMessage(), e);
        }
        throw new IllegalStateException(imageModel + " response does not contain image url");
    }

    private String buildStyledPrompt(String prompt, String style) {
        Map<String, String> styleMap = Map.of(
                "剪纸", "paper-cut art style, traditional Chinese folk art",
                "水墨", "Chinese ink wash painting style, traditional brush painting",
                "皮影", "Chinese shadow puppetry style, leather silhouette art",
                "刺绣", "Chinese embroidery art style, silk thread craft"
        );
        String styleDesc = styleMap.getOrDefault(style, "traditional Chinese intangible cultural heritage art style");
        return prompt + ", " + styleDesc + ", high quality, detailed";
    }

    private void updateTaskStatus(String taskId, String status, int progress, String resultUrl, String error) {
        ImageGenerationTask task = new ImageGenerationTask();
        task.setTaskId(taskId);
        task.setStatus(status);
        task.setProgress(progress);
        task.setResultUrl(resultUrl);
        task.setErrorMessage(trimToLength(error, ERROR_MESSAGE_MAX_LENGTH));
        taskMapper.updateStatus(task);
        cacheTaskStatus(taskId, status, progress, resultUrl, task.getErrorMessage());
    }

    private String shortenErrorMessage(Exception e) {
        if (e instanceof HttpStatusCodeException httpError) {
            return trimToLength(httpError.getResponseBodyAsString(), ERROR_MESSAGE_MAX_LENGTH);
        }

        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            message = e.getClass().getSimpleName();
        }
        return trimToLength(message, ERROR_MESSAGE_MAX_LENGTH);
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private void cacheTaskStatus(String taskId, String status, int progress, String resultUrl, String error) {
        String key = TASK_CACHE_PREFIX + taskId;
        redisTemplate.opsForHash().put(key, "taskId", taskId);
        redisTemplate.opsForHash().put(key, "status", status);
        redisTemplate.opsForHash().put(key, "progress", String.valueOf(progress));
        if (StringUtils.hasText(resultUrl)) {
            redisTemplate.opsForHash().put(key, "resultUrl", resultUrl);
        }
        if (StringUtils.hasText(error)) {
            redisTemplate.opsForHash().put(key, "errorMessage", error);
        }
        redisTemplate.expire(key, TASK_CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    private ImageGenerationTask getCachedTask(String taskId) {
        String key = TASK_CACHE_PREFIX + taskId;
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);
        if (values == null || values.isEmpty()) {
            return null;
        }

        ImageGenerationTask task = new ImageGenerationTask();
        task.setTaskId((String) values.get("taskId"));
        task.setStatus((String) values.get("status"));
        task.setResultUrl((String) values.get("resultUrl"));
        task.setErrorMessage((String) values.get("errorMessage"));
        Object progress = values.get("progress");
        task.setProgress(progress == null ? 0 : Integer.parseInt(progress.toString()));
        return task;
    }

    private void broadcastProgress(Long roomId, String taskId, int progress) {
        if (roomId == null) {
            return;
        }
        WebSocketMessage msg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.PROGRESS)
                .senderId("SYSTEM")
                .senderName("系统")
                .roomId(roomId)
                .content(String.valueOf(progress))
                .metadata(Map.of("taskId", taskId, "progress", progress))
                .build();
        sessionManager.broadcastToRoom(roomId, msg);
    }

    private void broadcastImageResult(Long roomId, String taskId, String imageUrl) {
        if (roomId == null) {
            return;
        }
        WebSocketMessage msg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.IMAGE)
                .senderId("SYSTEM")
                .senderName("系统")
                .roomId(roomId)
                .imageUrl(imageUrl)
                .metadata(Map.of("taskId", taskId))
                .build();
        sessionManager.broadcastToRoom(roomId, msg);
    }

    private void broadcastError(Long roomId, String taskId, String error) {
        if (roomId == null) {
            return;
        }
        WebSocketMessage msg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ERROR)
                .senderId("SYSTEM")
                .senderName("系统")
                .roomId(roomId)
                .content("图片生成失败: " + error)
                .metadata(Map.of("taskId", taskId))
                .build();
        sessionManager.broadcastToRoom(roomId, msg);
    }
}
