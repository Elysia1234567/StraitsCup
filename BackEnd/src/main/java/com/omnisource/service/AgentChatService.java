package com.omnisource.service;

import com.omnisource.entity.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AgentChatService {
    void handleUserMessage(Long roomId, Long userId, String content, String imageUrl, boolean searchEnabled);
    void handleUserMessage(Long roomId, Long userId, String content, String imageUrl, boolean searchEnabled, boolean ragEnabled);
    Flux<String> streamAgentResponse(Long roomId, Long agentId, List<ChatMessage> history, String userMessage, String imageUrl, boolean searchEnabled);
    Flux<String> streamAgentResponse(Long roomId, Long agentId, List<ChatMessage> history, String userMessage, String imageUrl, boolean searchEnabled, boolean ragEnabled);
}
