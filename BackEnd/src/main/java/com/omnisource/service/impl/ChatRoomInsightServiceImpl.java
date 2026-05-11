package com.omnisource.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisource.dto.response.ChatRoomInsightResponse;
import com.omnisource.dto.response.ChatRoomInsightResponse.ConfidenceView;
import com.omnisource.dto.response.ChatRoomInsightResponse.EvidenceSourceView;
import com.omnisource.dto.response.ChatRoomInsightResponse.RelationPathView;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.entity.ChatMessage;
import com.omnisource.entity.ChatRoom;
import com.omnisource.entity.ChatRoomMember;
import com.omnisource.service.ChatHistoryService;
import com.omnisource.service.ChatRoomInsightService;
import com.omnisource.service.ChatRoomService;
import com.omnisource.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatRoomInsightServiceImpl implements ChatRoomInsightService {

    private static final int HISTORY_LIMIT = 100;

    private final ChatRoomService chatRoomService;
    private final ChatHistoryService chatHistoryService;
    private final RagService ragService;
    private final ObjectMapper objectMapper;

    @Override
    public ChatRoomInsightResponse getRoomInsight(Long roomId) {
        ChatRoom room = chatRoomService.getRoomById(roomId);
        if (room == null) {
            return null;
        }

        List<ChatRoomMember> agentMembers = chatRoomService.getRoomAgentMembers(roomId);
        List<ChatMessage> history = chatHistoryService.getRecentHistory(roomId, HISTORY_LIMIT);
        ChatMessage latestQuestion = findLatest(history, "USER");
        ChatMessage latestAssistant = findLatest(history, "AGENT");
        ChatMessage evidenceMessage = firstNonNull(findLatestEvidenceMessage(history), latestAssistant);
        Map<String, Object> metadata = buildEvidenceMetadata(evidenceMessage);
        Map<String, Object> rag = asMap(metadata.get("rag"));
        Map<String, Object> webSearch = asMap(metadata.get("webSearch"));
        Map<String, Object> confidence = asMap(metadata.get("confidence"));

        List<EvidenceSourceView> evidenceSources = buildEvidenceSources(rag, webSearch);
        if (evidenceSources.isEmpty() && latestQuestion != null && StringUtils.hasText(latestQuestion.getContent())) {
            evidenceSources.addAll(buildRagEvidenceSources(latestQuestion.getContent()));
        }
        if (evidenceSources.isEmpty() && latestAssistant != null && StringUtils.hasText(latestAssistant.getContent())) {
            evidenceSources.add(buildConversationEvidence(latestAssistant));
        }
        ConfidenceView confidenceView = buildConfidenceView(confidence, rag, webSearch, evidenceSources);

        String summary = resolveSummary(confidenceView, webSearch, latestQuestion, latestAssistant, evidenceSources);
        List<String> knowledgeTags = buildKnowledgeTags(room, agentMembers, evidenceSources, metadata);
        List<RelationPathView> relationPaths = buildRelationPaths(room, latestQuestion, latestAssistant, evidenceSources, confidenceView);

        return ChatRoomInsightResponse.builder()
                .roomId(room.getId())
                .roomName(room.getName())
                .agentCount(agentMembers.size())
                .messageCount(chatHistoryService.countMessages(roomId))
                .latestQuestion(latestQuestion == null ? null : latestQuestion.getContent())
                .latestAnswer(latestAssistant == null ? null : latestAssistant.getContent())
                .latestAgentName(latestAssistant == null ? null : latestAssistant.getSenderName())
                .latestUpdateTime(resolveLatestTime(latestAssistant, latestQuestion))
                .summary(summary)
                .confidence(confidenceView)
                .evidenceSources(evidenceSources)
                .knowledgeTags(knowledgeTags)
                .relationPaths(relationPaths)
                .build();
    }

    private ChatMessage findLatest(List<ChatMessage> history, String senderType) {
        if (CollectionUtils.isEmpty(history)) {
            return null;
        }
        for (ChatMessage message : history) {
            if (senderType.equals(message.getSenderType())) {
                return message;
            }
        }
        return null;
    }

    private ChatMessage findLatestEvidenceMessage(List<ChatMessage> history) {
        if (CollectionUtils.isEmpty(history)) {
            return null;
        }
        for (ChatMessage message : history) {
            if ("AGENT".equals(message.getSenderType()) && hasEvidencePayload(message)) {
                return message;
            }
        }
        return null;
    }

    private boolean hasEvidencePayload(ChatMessage message) {
        if (message == null) {
            return false;
        }
        Map<String, Object> metadata = parseMetadata(message.getMetadata());
        Map<String, Object> searchResults = parseMetadata(message.getSearchResults());
        Map<String, Object> rag = asMap(metadata.get("rag"));
        Map<String, Object> webSearch = asMap(metadata.get("webSearch"));
        Map<String, Object> searchWebSearch = asMap(searchResults.get("webSearch"));
        return hasSources(rag)
                || hasSources(webSearch)
                || hasSources(searchWebSearch)
                || StringUtils.hasText(stringValue(webSearch.get("summary")))
                || StringUtils.hasText(stringValue(searchWebSearch.get("summary")));
    }

    private boolean hasSources(Map<String, Object> payload) {
        Object sources = payload.get("sources");
        return sources instanceof List<?> list && !list.isEmpty();
    }

    private Map<String, Object> buildEvidenceMetadata(ChatMessage message) {
        if (message == null) {
            return Map.of();
        }
        Map<String, Object> metadata = new LinkedHashMap<>(parseMetadata(message.getMetadata()));
        Map<String, Object> searchResults = parseMetadata(message.getSearchResults());
        Map<String, Object> webSearch = asMap(searchResults.get("webSearch"));
        if (!webSearch.isEmpty()) {
            Map<String, Object> existingWebSearch = asMap(metadata.get("webSearch"));
            if (existingWebSearch.isEmpty()) {
                metadata.put("webSearch", webSearch);
            }
        }
        if (!metadata.containsKey("confidence") && searchResults.containsKey("confidence")) {
            metadata.put("confidence", searchResults.get("confidence"));
        }
        if (!metadata.containsKey("rag") && searchResults.containsKey("rag")) {
            metadata.put("rag", searchResults.get("rag"));
        }
        if (!metadata.containsKey("webSearch") && searchResults.containsKey("webSearch")) {
            metadata.put("webSearch", webSearch);
        }
        return metadata;
    }

    private Map<String, Object> parseMetadata(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return result;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<EvidenceSourceView> buildEvidenceSources(Map<String, Object> rag, Map<String, Object> webSearch) {
        List<EvidenceSourceView> sources = new ArrayList<>();

        Object ragSourcesRaw = rag.get("sources");
        if (ragSourcesRaw instanceof List<?> ragSources) {
            for (Object raw : ragSources) {
                Map<String, Object> source = asMap(raw);
                sources.add(EvidenceSourceView.builder()
                        .id(stringValue(source.get("id")))
                        .title(defaultString(stringValue(source.get("title")), "RAG 证据"))
                        .provider(firstNonBlank(stringValue(source.get("category")), stringValue(source.get("region")), "RAG"))
                        .confidence(normalizeScore(source.get("score")))
                        .date(firstNonBlank(stringValue(source.get("date")), stringValue(source.get("publishDate"))))
                        .excerpt(defaultString(stringValue(source.get("excerpt")), stringValue(source.get("content"))))
                        .type(defaultString(stringValue(source.get("type")), "RAG"))
                        .url(stringValue(source.get("url")))
                        .build());
            }
        }

        Object webSourcesRaw = webSearch.get("sources");
        if (webSourcesRaw instanceof List<?> webSources) {
            for (Object raw : webSources) {
                Map<String, Object> source = asMap(raw);
                sources.add(EvidenceSourceView.builder()
                        .id(firstNonBlank(stringValue(source.get("url")), stringValue(source.get("id"))))
                        .title(defaultString(stringValue(source.get("title")), "网页来源"))
                        .provider(firstNonBlank(hostOf(stringValue(source.get("url"))), "网页"))
                        .confidence(normalizeScore(source.get("score")))
                        .date(stringValue(source.get("date")))
                        .excerpt(defaultString(stringValue(source.get("excerpt")), stringValue(source.get("content"))))
                        .type("网页")
                        .url(stringValue(source.get("url")))
                        .build());
            }
        }

        return sources;
    }

    private List<EvidenceSourceView> buildRagEvidenceSources(String question) {
        try {
            List<RagRetrievalResponse> retrievals = ragService.retrieve(question, 6);
            if (CollectionUtils.isEmpty(retrievals)) {
                return List.of();
            }
            List<EvidenceSourceView> sources = new ArrayList<>();
            for (RagRetrievalResponse item : retrievals) {
                Map<String, Object> itemMetadata = item.getMetadata() == null ? Map.of() : item.getMetadata();
                sources.add(EvidenceSourceView.builder()
                        .id(firstNonBlank(item.getId(), item.getTitle()))
                        .title(defaultString(item.getTitle(), "知识库检索结果"))
                        .provider(firstNonBlank(
                                stringValue(itemMetadata.get("source")),
                                stringValue(itemMetadata.get("path")),
                                stringValue(itemMetadata.get("category")),
                                stringValue(itemMetadata.get("region")),
                                "RAG 知识库"
                        ))
                        .confidence(normalizeScore(item.getScore()))
                        .date(firstNonBlank(stringValue(itemMetadata.get("date")), stringValue(itemMetadata.get("year"))))
                        .excerpt(truncate(item.getContent(), 180))
                        .type(firstNonBlank(stringValue(itemMetadata.get("type")), stringValue(itemMetadata.get("category")), "RAG"))
                        .url(firstNonBlank(stringValue(itemMetadata.get("url")), stringValue(itemMetadata.get("sourceUrl"))))
                        .build());
            }
            return sources;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private EvidenceSourceView buildConversationEvidence(ChatMessage latestAssistant) {
        return EvidenceSourceView.builder()
                .id(latestAssistant.getId() == null ? "latest-answer" : "message-" + latestAssistant.getId())
                .title(defaultString(latestAssistant.getSenderName(), "最近智能体回复"))
                .provider("群聊记录")
                .confidence(0.55)
                .date(latestAssistant.getCreateTime() == null ? null : latestAssistant.getCreateTime().toLocalDate().toString())
                .excerpt(truncate(latestAssistant.getContent(), 180))
                .type("对话")
                .build();
    }

    private ConfidenceView buildConfidenceView(Map<String, Object> confidence, Map<String, Object> rag,
                                               Map<String, Object> webSearch, List<EvidenceSourceView> evidenceSources) {
        Double score = normalizeScore(confidence.get("score"));
        int evidenceCount = evidenceSources == null ? 0 : evidenceSources.size();
        if (score == 0.0 && evidenceCount > 0) {
            double strongest = evidenceSources.stream()
                    .map(EvidenceSourceView::getConfidence)
                    .filter(value -> value != null && value > 0)
                    .max(Double::compareTo)
                    .orElse(0.0);
            double average = evidenceSources.stream()
                    .map(EvidenceSourceView::getConfidence)
                    .filter(value -> value != null && value > 0)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            double base = Math.max(strongest, average);
            if (base <= 0.0) {
                base = 0.62;
            }
            score = Math.min(0.92, Math.max(base, 0.68) + Math.min(0.10, evidenceCount * 0.02));
        } else if (score > 0.0 && evidenceCount > 0) {
            score = Math.min(0.95, score + Math.min(0.08, evidenceCount * 0.01));
        }
        String level = firstNonBlank(
                stringValue(confidence.get("level")),
                evidenceSources.isEmpty() ? "待生成" : (score >= 0.85 ? "高" : score >= 0.55 ? "中" : "低")
        );
        String reason = firstNonBlank(
                stringValue(confidence.get("reason")),
                stringValue(webSearch.get("summary")),
                evidenceSources.isEmpty() ? "当前房间暂无可展示的证据链" : "已基于 RAG、联网搜索或群聊记录生成证据链"
        );
        return ConfidenceView.builder()
                .score(score)
                .level(level)
                .reason(reason)
                .build();
    }

    private String resolveSummary(ConfidenceView confidence, Map<String, Object> webSearch,
                                  ChatMessage latestQuestion, ChatMessage latestAssistant,
                                  List<EvidenceSourceView> evidenceSources) {
        String webSummary = stringValue(webSearch.get("summary"));
        if (StringUtils.hasText(webSummary)) {
            return webSummary;
        }
        if (latestAssistant != null && StringUtils.hasText(latestAssistant.getContent())) {
            return truncate(latestAssistant.getContent(), 220);
        }
        if (latestQuestion != null && StringUtils.hasText(latestQuestion.getContent()) && !evidenceSources.isEmpty()) {
            return "已围绕「" + truncate(latestQuestion.getContent(), 48) + "」检索并汇总 "
                    + evidenceSources.size() + " 条知识证据。";
        }
        if (confidence != null && StringUtils.hasText(confidence.getReason())) {
            return confidence.getReason();
        }
        return "当前房间暂无证据链，发送问题后会自动生成摘要、证据与置信度。";
    }

    private List<String> buildKnowledgeTags(ChatRoom room, List<ChatRoomMember> agentMembers,
                                            List<EvidenceSourceView> evidenceSources, Map<String, Object> metadata) {
        Set<String> tags = new LinkedHashSet<>();
        if (StringUtils.hasText(room.getName())) {
            tags.add(room.getName());
        }
        for (ChatRoomMember member : agentMembers) {
            if (StringUtils.hasText(member.getDisplayName())) {
                tags.add(member.getDisplayName());
            }
        }
        for (EvidenceSourceView source : evidenceSources) {
            if (StringUtils.hasText(source.getProvider())) {
                tags.add(source.getProvider());
            }
            if (StringUtils.hasText(source.getType())) {
                tags.add(source.getType());
            }
        }
        Map<String, Object> rag = asMap(metadata.get("rag"));
        if (Boolean.TRUE.equals(rag.get("enabled"))) {
            tags.add("RAG");
        }
        Map<String, Object> webSearch = asMap(metadata.get("webSearch"));
        if (Boolean.TRUE.equals(webSearch.get("enabled"))) {
            tags.add("联网搜索");
        }
        tags.add("多智能体");
        return new ArrayList<>(tags).stream().limit(8).toList();
    }

    private List<RelationPathView> buildRelationPaths(ChatRoom room, ChatMessage latestQuestion, ChatMessage latestAssistant,
                                                      List<EvidenceSourceView> evidenceSources, ConfidenceView confidence) {
        List<RelationPathView> paths = new ArrayList<>();
        if (room != null && StringUtils.hasText(room.getName())) {
            paths.add(RelationPathView.builder().key("群聊主题").value(room.getName()).build());
        }
        if (latestQuestion != null && StringUtils.hasText(latestQuestion.getContent())) {
            paths.add(RelationPathView.builder().key("最新提问").value(truncate(latestQuestion.getContent(), 24)).build());
        }
        if (latestAssistant != null && StringUtils.hasText(latestAssistant.getSenderName())) {
            paths.add(RelationPathView.builder().key("最近回复").value(latestAssistant.getSenderName()).build());
        }
        if (!evidenceSources.isEmpty()) {
            paths.add(RelationPathView.builder().key("证据来源").value(evidenceSources.size() + " 条").build());
        }
        if (confidence != null) {
            paths.add(RelationPathView.builder().key("置信度")
                    .value(String.format("%.2f · %s", confidence.getScore() == null ? 0.0 : confidence.getScore(), confidence.getLevel()))
                    .build());
        }
        return paths;
    }

    private LocalDateTime resolveLatestTime(ChatMessage latestAssistant, ChatMessage latestQuestion) {
        if (latestAssistant != null && latestAssistant.getCreateTime() != null) {
            return latestAssistant.getCreateTime();
        }
        return latestQuestion == null ? null : latestQuestion.getCreateTime();
    }

    private Double normalizeScore(Object value) {
        if (value instanceof Number number) {
            double score = number.doubleValue();
            if (score > 1.0) {
                score = score / 100.0;
            }
            return Math.max(0.0, Math.min(1.0, score));
        }
        return 0.0;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private String hostOf(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception ignored) {
            return url;
        }
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + "...";
    }
}
