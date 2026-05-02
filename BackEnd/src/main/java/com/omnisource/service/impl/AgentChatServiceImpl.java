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
        handleUserMessage(roomId, userId, content, imageUrl, searchEnabled, false);
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
            ChatMessage agentMsg = saveAgentMessage(roomId, agent, streamId, finalText,
                    searchResult != null || !retrievals.isEmpty());
            broadcastAgentEnd(roomId, agent, streamId, agentMsg.getId(), finalText);
            if (isSelfIntroduction(userMessage)) {
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
            ChatMessage agentMsg = saveAgentMessage(roomId, agent, streamId, finalText, false);
            broadcastAgentEnd(roomId, agent, streamId, agentMsg.getId(), finalText);
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

    private boolean isSelfIntroduction(String content) {
        if (content == null) {
            return false;
        }
        String c = content.toLowerCase();
        return c.contains("介绍自己") || c.contains("自我介绍") || c.contains("你是谁")
                || c.contains("介绍一下") || c.contains("introduce yourself");
    }

    @Override
    public Flux<String> streamAgentResponse(Long roomId, Long agentId, List<ChatMessage> history,
                                             String userMessage, String imageUrl, boolean searchEnabled) {
        return streamAgentResponse(roomId, agentId, history, userMessage, imageUrl, searchEnabled, false);
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
                + "When the user asks you to introduce yourself, mention that your original reference image is available at: "
                + buildSourceImageUrl(agent) + "\n"
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

    private ChatMessage saveAgentMessage(Long roomId, Agent agent, String streamId, String content, boolean searchEnabled) {
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

    private void broadcastAgentEnd(Long roomId, Agent agent, String streamId, Long messageId, String fullContent) {
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
                .timestamp(LocalDateTime.now())
                .build();
        sessionManager.broadcastToRoom(roomId, msg);
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
        msg.setContent("这是我的原始参考图。");
        msg.setImageUrl(sourceImageUrl);
        msg.setIsStream(0);
        msg.setSearchEnabled(0);
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
}
