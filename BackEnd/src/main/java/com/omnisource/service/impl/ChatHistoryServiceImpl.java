package com.omnisource.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisource.entity.ChatMessage;
import com.omnisource.mapper.ChatMessageMapper;
import com.omnisource.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatMessageMapper chatMessageMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_HISTORY_KEY_PREFIX = "chat:history:";
    private static final int REDIS_HISTORY_LIMIT = 50;

    @Override
    public void addMessage(ChatMessage message) {
        chatMessageMapper.insert(message);
        addToRedisHistory(message);
    }

    private void addToRedisHistory(ChatMessage message) {
        try {
            String key = REDIS_HISTORY_KEY_PREFIX + message.getRoomId();
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(key, json);
            redisTemplate.opsForList().trim(key, 0, REDIS_HISTORY_LIMIT - 1);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Redis历史记录添加失败", e);
        }
    }

    @Override
    public List<ChatMessage> getRecentHistory(Long roomId, int limit) {
        try {
            String key = REDIS_HISTORY_KEY_PREFIX + roomId;
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, limit - 1);
            if (jsonList != null && !jsonList.isEmpty()) {
                List<ChatMessage> messages = new ArrayList<>();
                for (String json : jsonList) {
                    messages.add(objectMapper.readValue(json, ChatMessage.class));
                }
                return messages;
            }
        } catch (Exception e) {
            log.error("Redis历史记录读取失败", e);
        }
        return chatMessageMapper.selectRecentByRoomId(roomId, limit);
    }

    @Override
    public List<ChatMessage> getHistoryPage(Long roomId, int page, int size) {
        int offset = (page - 1) * size;
        return chatMessageMapper.selectByRoomIdWithPage(roomId, offset, size);
    }

    @Override
    public void clearHistory(Long roomId) {
        redisTemplate.delete(REDIS_HISTORY_KEY_PREFIX + roomId);
        chatMessageMapper.deleteByRoomId(roomId);
    }

    @Override
    public List<ChatMessage> getContextForAI(Long roomId, int limit) {
        List<ChatMessage> history = getRecentHistory(roomId, limit);
        List<ChatMessage> context = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if ("TEXT".equals(msg.getMessageType()) || "IMAGE".equals(msg.getMessageType())) {
                context.add(msg);
            }
        }
        return context;
    }

    @Override
    public void updateMessageContent(Long messageId, String content) {
        chatMessageMapper.updateContent(messageId, content);
    }
}
