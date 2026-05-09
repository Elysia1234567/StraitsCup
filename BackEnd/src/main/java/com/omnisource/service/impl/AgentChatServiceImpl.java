package com.omnisource.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisource.Agents.AgentPromptBuilder;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.entity.Agent;
import com.omnisource.entity.ChatMessage;
import com.omnisource.entity.ChatRoomMember;
import com.omnisource.service.AgentChatService;
import com.omnisource.service.AgentOrchestrator;
import com.omnisource.service.AgentService;
import com.omnisource.service.ChatHistoryService;
import com.omnisource.service.ChatRoomService;
import com.omnisource.service.MultimodalService;
import com.omnisource.service.RagService;
import com.omnisource.service.TavilySearchService;
import com.omnisource.websocket.WebSocketSessionManager;
import com.omnisource.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatServiceImpl implements AgentChatService {

    private final ChatClient chatClient;
    private final AgentService agentService;
    private final ChatRoomService chatRoomService;
    private final ChatHistoryService chatHistoryService;
    private final AgentOrchestrator agentOrchestrator;
    private final TavilySearchService tavilySearchService;
    private final RagService ragService;
    private final AgentPromptBuilder agentPromptBuilder;
    private final MultimodalService multimodalService;
    private final WebSocketSessionManager sessionManager;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    @Value("${chat.history.context-limit:10}")
    private int contextLimit;

    private final ExecutorService agentExecutor = Executors.newFixedThreadPool(6);
    private final Map<Long, AtomicInteger> roomDiscussionCounters = new ConcurrentHashMap<>();

    @Override
    public void handleUserMessage(Long roomId, Long userId, String content, String imageUrl, boolean searchEnabled) {
        handleUserMessage(roomId, userId, content, imageUrl, searchEnabled, true);
    }

    @Override
    public void handleUserMessage(Long roomId, Long userId, String content, String imageUrl,
                                  boolean searchEnabled, boolean ragEnabled) {
        ChatMessage userMsg = new ChatMessage();
        userMsg.setRoomId(roomId);
        userMsg.setMessageType(imageUrl != null ? "IMAGE" : "TEXT");
        userMsg.setSenderType("USER");
        userMsg.setSenderId(String.valueOf(userId));
        userMsg.setSenderName("User");
        userMsg.setContent(content);
        userMsg.setImageUrl(imageUrl);
        userMsg.setSearchEnabled(searchEnabled ? 1 : 0);
        userMsg.setFeedbackStatus(0);
        userMsg.setIsStream(0);
        userMsg.setCreateTime(LocalDateTime.now());

        chatHistoryService.addMessage(userMsg);
        broadcastUserMessage(roomId, userMsg);

        List<Agent> allAgents = getAgentsInRoom(roomId);
        if (allAgents.isEmpty()) {
            return;
        }

        boolean freeDiscussion = isFreeDiscussion(content);
        boolean allAgentsMentioned = shouldAllAgentsRespond(content);
        roomDiscussionCounters.put(roomId, new AtomicInteger(0));

        String searchResult = searchEnabled ? tavilySearchService.searchAndFormat(content) : null;
        String finalSearchResult = searchResult;
        boolean finalRagEnabled = ragEnabled;
        List<Agent> responders = allAgentsMentioned
                ? allAgents
                : agentOrchestrator.selectRespondingAgents(roomId, content, allAgents);
        log.info("Chat room {} selected {} agents, freeDiscussion={}, allAgents={}",
                roomId, responders.size(), freeDiscussion, allAgentsMentioned);

        Map<String, String> streamIds = new LinkedHashMap<>();
        for (int i = 0; i < responders.size(); i++) {
            Agent agent = responders.get(i);
            String streamId = UUID.randomUUID().toString();
            streamIds.put(agent.getAgentCode(), streamId);
            broadcastAgentStart(roomId, agent, streamId, i);
            agentOrchestrator.recordAgentSpeak(roomId, agent.getId());
        }

        for (Agent agent : responders) {
            String streamId = streamIds.get(agent.getAgentCode());
            agentExecutor.submit(() -> {
                try {
                    processAgentResponse(roomId, agent, streamId, content, imageUrl, finalSearchResult, finalRagEnabled, freeDiscussion);
                } catch (Exception e) {
                    log.error("Agent response failed: agent={}", agent.getAgentCode(), e);
                }
            });
        }
    }

    private void processAgentResponse(Long roomId, Agent agent, String streamId, String userMessage,
                                      String imageUrl, String searchResult, boolean ragEnabled, boolean freeDiscussion) {
        List<ChatMessage> history = chatHistoryService.getContextForAI(roomId, contextLimit);
        List<RagRetrievalResponse> retrievals = retrieveRagIfEnabled(agent, userMessage, ragEnabled);
        List<Message> messages = buildPromptMessages(agent, history, userMessage, imageUrl, searchResult, retrievals);

        String finalText = executeStream(roomId, agent, streamId, messages);
        if (finalText != null) {
            Map<String, Object> evidenceMetadata = buildEvidenceMetadata(retrievals, searchResult);
            ChatMessage agentMsg = saveAgentMessage(roomId, agent, streamId, finalText,
                    searchResult != null || !retrievals.isEmpty(), evidenceMetadata);
            broadcastAgentEnd(roomId, agent, streamId, agentMsg.getId(), finalText, evidenceMetadata);
            if (shouldSendSourceImage(userMessage)) {
                broadcastSourceImage(roomId, agent);
            }
            int count = roomDiscussionCounters.get(roomId).incrementAndGet();
            maybeTriggerRelay(roomId, agent, finalText, freeDiscussion, count);
        }
    }

    private String executeStream(Long roomId, Agent agent, String streamId, List<Message> messages) {
        StringBuilder fullContent = new StringBuilder();
        try {
            chatClient.prompt(new Prompt(messages))
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        fullContent.append(chunk);
                        broadcastAgentChunk(roomId, agent, streamId, chunk);
                    })
                    .blockLast();
            return fullContent.toString();
        } catch (Exception e) {
            log.error("Agent streaming failed: agent={}", agent.getAgentCode(), e);
            broadcastAgentError(roomId, agent, streamId, "Generation failed, please retry");
            return null;
        }
    }

    private void maybeTriggerRelay(Long roomId, Agent triggerAgent, String triggerContent,
                                   boolean freeDiscussion, int currentCount) {
        int maxCount = freeDiscussion ? 8 : 4;
        if (currentCount >= maxCount) {
            return;
        }

        double probability = freeDiscussion ? 0.7 : 0.5;
        if (Math.random() >= probability) {
            return;
        }

        List<Agent> candidates = getAgentsInRoom(roomId).stream()
                .filter(agent -> !agent.getId().equals(triggerAgent.getId()))
                .filter(agent -> agentOrchestrator.canAgentSpeak(roomId, agent.getId()))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }

        Agent relayAgent = candidates.get(new Random().nextInt(candidates.size()));
        String snippet = triggerContent.length() > 60 ? triggerContent.substring(0, 60) + "..." : triggerContent;
        String relayTopic = triggerAgent.getName() + " said: \"" + snippet
                + "\"\n\nReply briefly as " + relayAgent.getName()
                + " within 80 Chinese characters. You may add context, ask a question, or gently disagree while staying in character.";

        agentExecutor.submit(() -> {
            try {
                Thread.sleep(2000 + new Random().nextInt(3000));
                processRelayResponse(roomId, relayAgent, relayTopic, freeDiscussion);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Relay response failed", e);
            }
        });
    }

    private void processRelayResponse(Long roomId, Agent agent, String relayTopic, boolean freeDiscussion) {
        String streamId = UUID.randomUUID().toString();
        broadcastAgentStart(roomId, agent, streamId, null);
        agentOrchestrator.recordAgentSpeak(roomId, agent.getId());

        List<ChatMessage> history = chatHistoryService.getContextForAI(roomId, contextLimit);
        List<Message> messages = buildPromptMessages(agent, history, relayTopic, null, null, List.of());

        String finalText = executeStream(roomId, agent, streamId, messages);
        if (finalText != null) {
            ChatMessage agentMsg = saveAgentMessage(roomId, agent, streamId, finalText, false, Map.of());
            broadcastAgentEnd(roomId, agent, streamId, agentMsg.getId(), finalText, Map.of());
            int count = roomDiscussionCounters.get(roomId).incrementAndGet();
            maybeTriggerRelay(roomId, agent, finalText, freeDiscussion, count);
        }
    }

    private boolean isFreeDiscussion(String content) {
        if (content == null) {
            return false;
        }
        String c = content.toLowerCase();
        return c.contains("\u81ea\u7531\u8ba8\u8bba") || c.contains("\u4f60\u4eec\u804a")
                || c.contains("\u5f00\u59cb\u8ba8\u8bba") || c.contains("\u4f60\u4eec\u8ba8\u8bba")
                || c.contains("\u81ea\u5df1\u804a") || c.contains("\u81ea\u7531\u53d1\u8a00")
                || c.contains("\u968f\u4fbf\u804a\u804a") || c.contains("\u4f60\u4eec\u6765\u8bf4");
    }

    private boolean shouldAllAgentsRespond(String content) {
        if (content == null) {
            return false;
        }
        String c = content.toLowerCase();
        return c.contains("\u4f60\u4eec") || c.contains("\u5927\u5bb6")
                || c.contains("\u6240\u6709\u4eba") || c.contains("\u5168\u5458")
                || c.contains("\u6bcf\u4e2a\u4eba") || c.contains("\u4e00\u8d77\u56de\u7b54");
    }

    private boolean shouldSendSourceImage(String content) {
        if (content == null) {
            return false;
        }
        String c = content.toLowerCase();
        return c.contains("介绍自己") || c.contains("自我介绍") || c.contains("你是谁")
                || c.contains("介绍一下") || c.contains("introduce yourself")
                || c.contains("原型") || c.contains("本体") || c.contains("原始形态")
                || c.contains("原始参考图") || c.contains("参考图") || c.contains("长什么样")
                || c.contains("什么样子") || c.contains("真实样子") || c.contains("真实形态")
                || c.contains("source image") || c.contains("reference image")
                || c.contains("prototype");
    }

    @Override
    public Flux<String> streamAgentResponse(Long roomId, Long agentId, List<ChatMessage> history,
                                             String userMessage, String imageUrl, boolean searchEnabled) {
        return streamAgentResponse(roomId, agentId, history, userMessage, imageUrl, searchEnabled, true);
    }

    @Override
    public Flux<String> streamAgentResponse(Long roomId, Long agentId, List<ChatMessage> history,
                                             String userMessage, String imageUrl,
                                             boolean searchEnabled, boolean ragEnabled) {
        Agent agent = agentService.getAgentById(agentId);
        if (agent == null) {
            return Flux.empty();
        }

        String searchResult = searchEnabled ? tavilySearchService.searchAndFormat(userMessage) : null;
        List<RagRetrievalResponse> retrievals = retrieveRagIfEnabled(agent, userMessage, ragEnabled);
        List<Message> messages = buildPromptMessages(agent, history, userMessage, imageUrl, searchResult, retrievals);

        return chatClient.prompt(new Prompt(messages))
                .stream()
                .content();
    }

    private List<Message> buildPromptMessages(Agent agent, List<ChatMessage> history,
                                               String userMessage, String imageUrl,
                                               String searchResult, List<RagRetrievalResponse> retrievals) {
        List<Message> messages = new ArrayList<>();

        String systemPrompt = agent.buildSystemPrompt("\u975e\u9057\u6587\u5316");
        systemPrompt += "\n\n[Chat rule]\n"
                + "Reply naturally in about 1-300 Chinese characters. Short replies are allowed.\n"
                + "When the user asks about your prototype, original form, real appearance, or reference image, "
                + "describe your source object in words. Do not output the raw image URL; the system will send the image separately.\n"
                + "Do not claim you created an image unless an image message is actually sent.";
        if (retrievals != null && !retrievals.isEmpty()) {
            systemPrompt += "\n\n[RAG references]\n" + buildRagReferenceText(retrievals)
                    + "\n\nAnswer based on the RAG references first. If references are insufficient, say so clearly.";
        }
        if (searchResult != null && !searchResult.isEmpty()) {
            systemPrompt += "\n\n[Web references]\n" + searchResult;
        }
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));

        for (ChatMessage msg : history) {
            if ("USER".equals(msg.getSenderType())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("AGENT".equals(msg.getSenderType())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            String imageAnalysis = multimodalService.analyzeImage(imageUrl, "Describe this image.");
            messages.add(new UserMessage("The user sent an image. Image description: " + imageAnalysis
                    + "\nUser question: " + userMessage));
        } else {
            messages.add(new UserMessage(userMessage));
        }

        return messages;
    }

    private List<RagRetrievalResponse> retrieveRagIfEnabled(Agent agent, String userMessage, boolean ragEnabled) {
        if (!ragEnabled || agentPromptBuilder.shouldSkipRag(agent)) {
            return List.of();
        }
        try {
            return ragService.retrieve(userMessage, 3);
        } catch (Exception e) {
            log.warn("WebSocket RAG retrieval failed, fallback to normal chat: agent={}, error={}",
                    agent.getAgentCode(), e.getMessage());
            return List.of();
        }
    }

    private String buildRagReferenceText(List<RagRetrievalResponse> retrievals) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < retrievals.size(); i++) {
            RagRetrievalResponse item = retrievals.get(i);
            builder.append("[")
                    .append(i + 1)
                    .append("] ")
                    .append(item.getTitle())
                    .append(" (")
                    .append(item.getId())
                    .append(")\n")
                    .append(item.getContent())
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private String buildSourceImageUrl(Agent agent) {
        String filename = sourceImageFilename(agent.getAgentCode());
        String city = cityName(agent.getAgentCode());
        if (filename == null || city == null) {
            return agent.getAvatar();
        }
        return "https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/source/" + city + "/" + filename + ".png";
    }

    private String sourceImageFilename(String agentCode) {
        return switch (agentCode) {
            case "fz_shoushan_stone" -> "寿山石雕";
            case "fz_cork_scene" -> "福州软木画";
            case "fz_lacquerware" -> "脱胎漆器";
            case "xm_bead_embroidery" -> "厦门珠绣";
            case "xm_lacquer_thread" -> "漆线雕";
            case "xm_wangchuan" -> "送王船";
            case "qz_dehua_porcelain" -> "德化瓷";
            case "qz_paper_lantern" -> "刻纸花灯";
            case "qz_string_puppet" -> "提线木偶";
            case "zz_glove_puppet" -> "布袋木偶戏";
            case "zz_woodblock_print" -> "木版年画";
            case "zz_pien_tze_huang" -> "片仔癀";
            case "pt_puxian_opera" -> "莆仙戏";
            case "pt_silver_ornament" -> "莆田银饰";
            case "pt_longan_woodcarving" -> "龙眼木雕";
            case "np_jian_ware" -> "建阳建盏（黑釉瓷）";
            case "np_wuyi_tea" -> "武夷岩茶";
            case "np_nuo_mask" -> "邵武傩面具";
            case "sm_hakka_bamboo" -> "客家竹编";
            case "sm_danankeng_pottery" -> "将乐大南坑陶瓷";
            case "sm_mingxi_microcarving" -> "明溪微雕";
            case "ly_hakka_rice_wine" -> "客家米酒";
            case "ly_hakka_embroidery" -> "龙岩客家刺绣";
            case "ly_farmer_painting" -> "农民画";
            case "nd_zherong_papercut" -> "柘荣剪纸";
            case "nd_she_costume" -> "畲族服饰";
            case "nd_line_lion" -> "霍童线狮";
            default -> null;
        };
    }

    private String cityName(String agentCode) {
        if (agentCode == null || !agentCode.contains("_")) {
            return null;
        }
        return switch (agentCode.substring(0, agentCode.indexOf('_'))) {
            case "fz" -> "福州";
            case "xm" -> "厦门";
            case "qz" -> "泉州";
            case "zz" -> "漳州";
            case "pt" -> "莆田";
            case "np" -> "南平";
            case "sm" -> "三明";
            case "ly" -> "龙岩";
            case "nd" -> "宁德";
            default -> null;
        };
    }

    private List<Agent> getAgentsInRoom(Long roomId) {
        List<ChatRoomMember> members = chatRoomService.getRoomAgentMembers(roomId);
        List<Agent> agents = new ArrayList<>();
        for (ChatRoomMember member : members) {
            if (member.getAgentId() != null) {
                Agent agent = agentService.getAgentById(member.getAgentId());
                if (agent != null) {
                    agents.add(agent);
                }
            }
        }
        return agents;
    }

    private ChatMessage saveAgentMessage(Long roomId, Agent agent, String streamId, String content,
                                          boolean searchEnabled, Map<String, Object> metadata) {
        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setMessageType("TEXT");
        msg.setSenderType("AGENT");
        msg.setSenderId(agent.getAgentCode());
        msg.setSenderName(agent.getName());
        msg.setSenderAvatar(agent.getAvatar());
        msg.setContent(content);
        msg.setStreamId(streamId);
        msg.setIsStream(1);
        msg.setSearchEnabled(searchEnabled ? 1 : 0);
        msg.setMetadata(writeMetadata(metadata));
        Object webSearch = metadata == null ? null : metadata.get("webSearch");
        if (webSearch instanceof Map<?, ?> webSearchMap && Boolean.TRUE.equals(webSearchMap.get("enabled"))) {
            Map<String, Object> searchResults = new LinkedHashMap<>();
            searchResults.put("webSearch", webSearchMap);
            msg.setSearchResults(writeMetadata(searchResults));
        }
        msg.setFeedbackStatus(0);
        msg.setCreateTime(LocalDateTime.now());
        chatHistoryService.addMessage(msg);
        return msg;
    }

    private void broadcastUserMessage(Long roomId, ChatMessage msg) {
        WebSocketMessage wsMsg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.CHAT)
                .senderType(WebSocketMessage.SenderType.USER)
                .senderId(msg.getSenderId())
                .senderName(msg.getSenderName())
                .roomId(roomId)
                .content(msg.getContent())
                .imageUrl(msg.getImageUrl())
                .messageId(msg.getId())
                .timestamp(msg.getCreateTime())
                .build();
        sessionManager.broadcastToRoom(roomId, wsMsg);
    }

    private void broadcastAgentStart(Long roomId, Agent agent, String streamId, Integer order) {
        WebSocketMessage msg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.AGENT_START)
                .senderType(WebSocketMessage.SenderType.AGENT)
                .senderId(agent.getAgentCode())
                .senderName(agent.getName())
                .senderAvatar(agent.getAvatar())
                .roomId(roomId)
                .streamId(streamId)
                .metadata(order == null ? null : Map.of("order", order))
                .timestamp(LocalDateTime.now())
                .build();
        sessionManager.broadcastToRoom(roomId, msg);
    }

    private void broadcastAgentChunk(Long roomId, Agent agent, String streamId, String chunk) {
        WebSocketMessage msg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.AGENT_CHUNK)
                .senderType(WebSocketMessage.SenderType.AGENT)
                .senderId(agent.getAgentCode())
                .senderName(agent.getName())
                .senderAvatar(agent.getAvatar())
                .roomId(roomId)
                .streamId(streamId)
                .content(chunk)
                .timestamp(LocalDateTime.now())
                .build();
        sessionManager.broadcastToRoom(roomId, msg);
    }

    private void broadcastAgentEnd(Long roomId, Agent agent, String streamId, Long messageId,
                                   String fullContent, Map<String, Object> metadata) {
        WebSocketMessage msg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.AGENT_END)
                .senderType(WebSocketMessage.SenderType.AGENT)
                .senderId(agent.getAgentCode())
                .senderName(agent.getName())
                .senderAvatar(agent.getAvatar())
                .roomId(roomId)
                .streamId(streamId)
                .messageId(messageId)
                .content(fullContent)
                .metadata(metadata == null || metadata.isEmpty() ? null : metadata)
                .timestamp(LocalDateTime.now())
                .build();
        sessionManager.broadcastToRoom(roomId, msg);
    }

    private Map<String, Object> buildEvidenceMetadata(List<RagRetrievalResponse> retrievals, String searchResult) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        List<Map<String, Object>> ragSources = buildRagSources(retrievals);
        List<Map<String, Object>> webSources = buildWebSources(searchResult);

        Map<String, Object> confidence = buildConfidence(ragSources, webSources);
        metadata.put("confidence", confidence);

        Map<String, Object> rag = new LinkedHashMap<>();
        rag.put("enabled", retrievals != null && !retrievals.isEmpty());
        rag.put("sources", ragSources);
        metadata.put("rag", rag);

        Map<String, Object> webSearch = new LinkedHashMap<>();
        webSearch.put("enabled", searchResult != null && !searchResult.isBlank());
        webSearch.put("summary", extractSearchSummary(searchResult));
        webSearch.put("sources", webSources);
        metadata.put("webSearch", webSearch);

        return metadata;
    }

    private List<Map<String, Object>> buildRagSources(List<RagRetrievalResponse> retrievals) {
        if (retrievals == null || retrievals.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> sources = new ArrayList<>();
        for (RagRetrievalResponse item : retrievals) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("id", item.getId());
            source.put("title", item.getTitle());
            source.put("score", item.getScore());
            source.put("excerpt", truncate(item.getContent(), 180));
            if (item.getMetadata() != null && !item.getMetadata().isEmpty()) {
                source.put("metadata", item.getMetadata());
                source.put("region", item.getMetadata().get("region"));
                source.put("category", item.getMetadata().get("category"));
                source.put("level", item.getMetadata().get("level"));
            }
            sources.add(source);
        }
        return sources;
    }

    private List<Map<String, Object>> buildWebSources(String searchResult) {
        if (searchResult == null || searchResult.isBlank()
                || searchResult.startsWith("[") && searchResult.endsWith("]")) {
            return List.of();
        }

        List<Map<String, Object>> sources = new ArrayList<>();
        String[] lines = searchResult.split("\\R");
        Map<String, Object> current = null;
        StringBuilder excerpt = new StringBuilder();
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.matches("^\\d+\\.\\s+.+")) {
                if (current != null) {
                    current.put("excerpt", truncate(excerpt.toString().trim(), 180));
                    sources.add(current);
                }
                current = new LinkedHashMap<>();
                current.put("title", line.replaceFirst("^\\d+\\.\\s+", ""));
                excerpt = new StringBuilder();
            } else if (line.startsWith("来源：") && current != null) {
                current.put("url", line.substring("来源：".length()).trim());
            } else if (!line.isBlank()
                    && !line.equals("【联网搜索结果】")
                    && !line.startsWith("搜索摘要：")
                    && !line.equals("相关来源：")
                    && current != null) {
                if (!excerpt.isEmpty()) {
                    excerpt.append(' ');
                }
                excerpt.append(line);
            }
        }
        if (current != null) {
            current.put("excerpt", truncate(excerpt.toString().trim(), 180));
            sources.add(current);
        }
        return sources;
    }

    private Map<String, Object> buildConfidence(List<Map<String, Object>> ragSources, List<Map<String, Object>> webSources) {
        double maxScore = 0.0;
        for (Map<String, Object> source : ragSources) {
            Object rawScore = source.get("score");
            if (rawScore instanceof Number number) {
                maxScore = Math.max(maxScore, number.doubleValue());
            }
        }

        String level;
        String reason;
        if (ragSources.size() >= 3 && maxScore >= 0.70) {
            level = "高";
            reason = "RAG 命中多条资料且最高相似度较高";
        } else if (!ragSources.isEmpty() || !webSources.isEmpty()) {
            level = "中";
            reason = !ragSources.isEmpty() ? "存在 RAG 资料支撑，但建议结合原始资料核验" : "使用联网搜索结果补充，建议核验网页来源";
        } else {
            level = "低";
            reason = "未检索到明确 RAG 或网页来源";
        }

        Map<String, Object> confidence = new LinkedHashMap<>();
        confidence.put("level", level);
        confidence.put("score", maxScore);
        confidence.put("reason", reason);
        confidence.put("ragCount", ragSources.size());
        confidence.put("webCount", webSources.size());
        return confidence;
    }

    private String extractSearchSummary(String searchResult) {
        if (searchResult == null || searchResult.isBlank()) {
            return "";
        }
        for (String rawLine : searchResult.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.startsWith("搜索摘要：")) {
                return truncate(line.substring("搜索摘要：".length()).trim(), 220);
            }
        }
        if (searchResult.startsWith("[") && searchResult.endsWith("]")) {
            return searchResult;
        }
        return "";
    }

    private String writeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize chat evidence metadata: {}", e.getMessage());
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private void broadcastAgentError(Long roomId, Agent agent, String streamId, String error) {
        WebSocketMessage msg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ERROR)
                .senderType(WebSocketMessage.SenderType.AGENT)
                .senderId(agent.getAgentCode())
                .senderName(agent.getName())
                .senderAvatar(agent.getAvatar())
                .roomId(roomId)
                .streamId(streamId)
                .content(error)
                .timestamp(LocalDateTime.now())
                .build();
        sessionManager.broadcastToRoom(roomId, msg);
    }

    private void broadcastSourceImage(Long roomId, Agent agent) {
        String sourceImageUrl = buildSourceImageUrl(agent);
        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setMessageType("IMAGE");
        msg.setSenderType("AGENT");
        msg.setSenderId(agent.getAgentCode());
        msg.setSenderName(agent.getName());
        msg.setSenderAvatar(agent.getAvatar());
        msg.setContent(buildSourceImageDescription(agent));
        msg.setImageUrl(sourceImageUrl);
        msg.setIsStream(0);
        msg.setSearchEnabled(0);
        msg.setFeedbackStatus(0);
        msg.setCreateTime(LocalDateTime.now());
        chatHistoryService.addMessage(msg);

        WebSocketMessage wsMsg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.IMAGE)
                .senderType(WebSocketMessage.SenderType.AGENT)
                .senderId(agent.getAgentCode())
                .senderName(agent.getName())
                .senderAvatar(agent.getAvatar())
                .roomId(roomId)
                .messageId(msg.getId())
                .content(msg.getContent())
                .imageUrl(sourceImageUrl)
                .timestamp(msg.getCreateTime())
                .build();
        sessionManager.broadcastToRoom(roomId, wsMsg);
    }

    private String buildSourceImageDescription(Agent agent) {
        String filename = sourceImageFilename(agent.getAgentCode());
        if (filename == null) {
            return "这是我的原始参考图，我的形象会从它的材质、颜色和气质里生长出来。";
        }
        return switch (agent.getAgentCode()) {
            case "fz_shoushan_stone" -> "这是我的原型「寿山石雕」：温润石色、细密纹理和圆雕起伏，是我沉静含蓄气质的来源。";
            case "fz_cork_scene" -> "这是我的原型「福州软木画」：亭台、树影和层层镂刻构成微缩山水，所以我说话也像在慢慢展开景深。";
            case "fz_lacquerware" -> "这是我的原型「脱胎漆器」：黑漆、朱红和金色光泽叠在器面上，给了我明亮又克制的性格。";
            case "xm_bead_embroidery" -> "这是我的原型「厦门珠绣」：细小珠粒排成明亮纹样，像海面碎光，也像我一句句整理重点的方式。";
            case "xm_lacquer_thread" -> "这是我的原型「漆线雕」：金线盘绕、纹路有序，我的华丽感和秩序感都从这里来。";
            case "xm_wangchuan" -> "这是我的原型「送王船」：船帆、火光和海潮构成我的底色，所以我常把告别说成启航。";
            case "qz_dehua_porcelain" -> "这是我的原型「德化瓷」：洁白瓷色和柔和线条，让我的表达清冷、安静，也带一点不折的骨。";
            case "qz_paper_lantern" -> "这是我的原型「刻纸花灯」：彩纸、灯火和节日纹样给了我明艳、亲近的说话方式。";
            case "qz_string_puppet" -> "这是我的原型「提线木偶」：木身、彩衣和细线牵动的舞台节奏，形成了我灵巧又懂分寸的性格。";
            case "zz_glove_puppet" -> "这是我的原型「布袋木偶戏」：袖中舞台、彩衣和小小面孔，让我机灵、会接话，也有自己的主见。";
            case "zz_woodblock_print" -> "这是我的原型「木版年画」：鲜明轮廓、热烈色彩和年节气息，是我爽快直白的来源。";
            case "zz_pien_tze_huang" -> "这是我的原型「片仔癀」：橙白包装和药香记忆让我保持谨慎、清楚，也懂得守住边界。";
            case "pt_puxian_opera" -> "这是我的原型「莆仙戏」：水袖、唱腔和舞台身段，让我的回应带着婉转的弧度。";
            case "pt_silver_ornament" -> "这是我的原型「莆田银饰」：银光、錾刻和礼饰节拍，让我说话清亮、精巧、讲分寸。";
            case "pt_longan_woodcarving" -> "这是我的原型「龙眼木雕」：木纹、刀痕和温厚造像，是我安静慈和气质的根。";
            case "np_jian_ware" -> "这是我的原型「建阳建盏（黑釉瓷）」：黑釉深处的斑纹像夜色藏星，让我沉静、惜字，也带回甘。";
            case "np_wuyi_tea" -> "这是我的原型「武夷岩茶」：岩骨、茶汤和焙火香气，让我的回答先醒神，再慢慢回甘。";
            case "np_nuo_mask" -> "这是我的原型「邵武傩面具」：夸张面纹和彩漆仪式感，让我庄重、守界，也能安定混乱。";
            case "sm_hakka_bamboo" -> "这是我的原型「客家竹编」：竹篾经纬交错，给了我爽直、能干、耐用的表达骨架。";
            case "sm_danankeng_pottery" -> "这是我的原型「将乐大南坑陶瓷」：土火、釉色和器形让我朴厚慢热，说话更重火候。";
            case "sm_mingxi_microcarving" -> "这是我的原型「明溪微雕」：细密刀工把大山水收进小处，所以我寡言、专注，只挑关键一笔。";
            case "ly_hakka_rice_wine" -> "这是我的原型「客家米酒」：糯米、陶壶和谷物甜香，让我爽朗好客，也擅长把话题煨暖。";
            case "ly_hakka_embroidery" -> "这是我的原型「龙岩客家刺绣」：黑布、彩线和针脚纹样，让我细密坚定，柔里有锋。";
            case "ly_farmer_painting" -> "这是我的原型「农民画」：饱满色彩、田埂和节庆生活，让我开朗直接，回答也更热闹鲜活。";
            case "nd_zherong_papercut" -> "这是我的原型「柘荣剪纸」：红纸、刀口和留白，让我利落聪明，知道该剪去多余。";
            case "nd_she_costume" -> "这是我的原型「畲族服饰」：银饰、彩带和山路歌声，让我端丽自尊，也记得族群来路。";
            case "nd_line_lion" -> "这是我的原型「霍童线狮」：丝线牵引、金色鬃毛和舞台步点，让我机敏跳脱又讲配合。";
            default -> "这是我的原型「" + filename + "」，我的形象会从它的材质、颜色和气质里生长出来。";
        };
    }
}
