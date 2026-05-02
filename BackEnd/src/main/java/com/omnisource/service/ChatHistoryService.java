package com.omnisource.service;

import com.omnisource.entity.ChatMessage;

import java.util.List;

public interface ChatHistoryService {
    void addMessage(ChatMessage message);
    List<ChatMessage> getRecentHistory(Long roomId, int limit);
    List<ChatMessage> getHistoryPage(Long roomId, int page, int size);
    void clearHistory(Long roomId);
    List<ChatMessage> getContextForAI(Long roomId, int limit);
    void updateMessageContent(Long messageId, String content);
}
