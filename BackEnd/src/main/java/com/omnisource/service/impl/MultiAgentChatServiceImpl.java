package com.omnisource.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisource.Agents.AgentDefaults;
import com.omnisource.Agents.AgentPromptBuilder;
import com.omnisource.dto.request.MultiAgentChatRequest;
import com.omnisource.dto.response.AgentReplyResponse;
import com.omnisource.dto.response.AgentTraceResponse;
import com.omnisource.dto.response.ConfidenceAssessmentResponse;
import com.omnisource.dto.response.ConflictInsightResponse;
import com.omnisource.dto.response.MultiAgentChatResponse;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.entity.Agent;
import com.omnisource.service.AgentService;
import com.omnisource.service.MultiAgentChatService;
import com.omnisource.service.RagService;
import com.omnisource.service.TavilySearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MultiAgentChatServiceImpl implements MultiAgentChatService {

    private static final String REDIS_SESSION_KEY_PREFIX = "chat:multi-agent:session:";

    private final ChatClient chatClient;
    private final RagService ragService;
    private final AgentService agentService;
    private final TavilySearchService tavilySearchService;
    private final AgentPromptBuilder agentPromptBuilder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor;
    private final int agentTimeoutSeconds;
    private final int sessionTtlDays;

    public MultiAgentChatServiceImpl(
            ChatModel chatModel,
            RagService ragService,
            AgentService agentService,
            TavilySearchService tavilySearchService,
            AgentPromptBuilder agentPromptBuilder,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Qualifier("taskExecutor") Executor taskExecutor,
            @Value("${chat.multi-agent.agent-timeout-seconds:20}") int agentTimeoutSeconds,
            @Value("${chat.multi-agent.session-ttl-days:7}") int sessionTtlDays
    ) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.ragService = ragService;
        this.agentService = agentService;
        this.tavilySearchService = tavilySearchService;
        this.agentPromptBuilder = agentPromptBuilder;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.agentTimeoutSeconds = Math.max(5, agentTimeoutSeconds);
        this.sessionTtlDays = Math.max(1, sessionTtlDays);
    }

    @Override
    public MultiAgentChatResponse chat(MultiAgentChatRequest request) {
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId().trim()
                : "sess_" + UUID.randomUUID();
        int topK = request.getTopK() != null && request.getTopK() > 0 ? request.getTopK() : 3;

        List<Agent> agents = resolveAgents(request.getAgentCodes());
        List<RagRetrievalResponse> retrievals = needsRag(agents)
                ? ragService.retrieve(request.getQuery(), topK)
                : List.of();
        String webSearchResult = shouldUseWebSearch(request, agents)
                ? tavilySearchService.searchAndFormat(request.getQuery())
                : null;

        List<CompletableFuture<AgentReplyResponse>> futures = agents.stream()
                .map(agent -> CompletableFuture
                        .supplyAsync(() -> buildAgentReply(agent, request, retrievals, webSearchResult), taskExecutor)
                        .completeOnTimeout(buildTimeoutReply(agent), agentTimeoutSeconds, TimeUnit.SECONDS)
                        .exceptionally(error -> buildFailureReply(agent)))
                .toList();

        List<AgentReplyResponse> agentReplies = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        ConflictInsightResponse conflictInsight = detectConflict(request.getQuery(), agentReplies);
        ConfidenceAssessmentResponse confidenceAssessment = assessConfidence(retrievals, webSearchResult, agentReplies);
        List<AgentTraceResponse> evidenceChain = buildEvidenceChain(agentReplies);

        MultiAgentChatResponse response = MultiAgentChatResponse.builder()
                .sessionId(sessionId)
                .query(request.getQuery())
                .heritageId(request.getHeritageId())
                .finalAnswer(agentPromptBuilder.buildFinalAnswer(agentReplies, conflictInsight, confidenceAssessment))
                .agentReplies(agentReplies)
                .retrievals(retrievals)
                .webSearchResult(webSearchResult)
                .evidenceChain(evidenceChain)
                .conflictInsight(conflictInsight)
                .confidenceAssessment(confidenceAssessment)
                .build();

        saveSessionResult(sessionId, response);
        return response;
    }

    @Override
    public MultiAgentChatResponse getSessionResult(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        String redisValue = redisTemplate.opsForValue().get(redisSessionKey(sessionId.trim()));
        if (!StringUtils.hasText(redisValue)) {
            return null;
        }

        try {
            return objectMapper.readValue(redisValue, MultiAgentChatResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveSessionResult(String sessionId, MultiAgentChatResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(redisSessionKey(sessionId), json, sessionTtlDays, TimeUnit.DAYS);
        } catch (Exception ignored) {
            // keep chat flow alive
        }
    }

    private String redisSessionKey(String sessionId) {
        return REDIS_SESSION_KEY_PREFIX + sessionId;
    }

    private List<Agent> resolveAgents(List<String> requestedCodes) {
        List<String> codes = CollectionUtils.isEmpty(requestedCodes)
                ? AgentDefaults.DEFAULT_AGENT_CODES
                : requestedCodes;

        LinkedHashSet<String> uniqueCodes = codes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Agent> agents = uniqueCodes.stream()
                .map(agentService::getAgentByCode)
                .filter(agent -> agent != null && (agent.getStatus() == null || agent.getStatus() == 1))
                .toList();

        if (!agents.isEmpty()) {
            return agents;
        }
        return agentService.getAllActiveAgents().stream().limit(2).toList();
    }

    private boolean needsRag(List<Agent> agents) {
        return agents.stream().anyMatch(agent -> !agentPromptBuilder.shouldSkipRag(agent));
    }

    private boolean shouldUseWebSearch(MultiAgentChatRequest request, List<Agent> agents) {
        return Boolean.TRUE.equals(request.getSearchEnabled()) && needsRag(agents);
    }

    private AgentReplyResponse buildAgentReply(
            Agent agent,
            MultiAgentChatRequest request,
            List<RagRetrievalResponse> retrievals,
            String webSearchResult
    ) {
        boolean skipRag = agentPromptBuilder.shouldSkipRag(agent);
        List<RagRetrievalResponse> agentRetrievals = skipRag ? List.of() : retrievals;
        String agentWebSearchResult = skipRag ? null : webSearchResult;

        String content = chatClient.prompt()
                .user(agentPromptBuilder.buildAgentPrompt(agent, request, agentRetrievals, agentWebSearchResult))
                .call()
                .content();

        return AgentReplyResponse.builder()
                .agentCode(agent.getAgentCode())
                .title(agent.getName())
                .content(StringUtils.hasText(content) ? content : "Empty response, downgraded.")
                .references(agentRetrievals.stream().map(RagRetrievalResponse::getId).collect(Collectors.toList()))
                .evidenceChunks(agentRetrievals)
                .searchUsed(StringUtils.hasText(agentWebSearchResult))
                .build();
    }

    private AgentReplyResponse buildTimeoutReply(Agent agent) {
        return AgentReplyResponse.builder()
                .agentCode(agent.getAgentCode())
                .title(agent.getName())
                .content("Response timed out, downgraded.")
                .references(List.of())
                .evidenceChunks(List.of())
                .searchUsed(false)
                .build();
    }

    private AgentReplyResponse buildFailureReply(Agent agent) {
        return AgentReplyResponse.builder()
                .agentCode(agent.getAgentCode())
                .title(agent.getName())
                .content("Response failed, downgraded.")
                .references(List.of())
                .evidenceChunks(List.of())
                .searchUsed(false)
                .build();
    }

    private List<AgentTraceResponse> buildEvidenceChain(List<AgentReplyResponse> agentReplies) {
        List<AgentTraceResponse> traces = new ArrayList<>();
        for (AgentReplyResponse reply : agentReplies) {
            traces.add(AgentTraceResponse.builder()
                    .agentCode(reply.getAgentCode())
                    .title(reply.getTitle())
                    .evidenceChunks(reply.getEvidenceChunks())
                    .viewpoint(extractViewpoint(reply.getContent()))
                    .conclusion(reply.getContent())
                    .build());
        }
        return traces;
    }

    private ConflictInsightResponse detectConflict(String query, List<AgentReplyResponse> agentReplies) {
        if (CollectionUtils.isEmpty(agentReplies) || agentReplies.size() < 2) {
            return ConflictInsightResponse.builder()
                    .conflictDetected(false)
                    .divergencePoints(List.of())
                    .verificationPoints(List.of())
                    .conflictingAgents(List.of())
                    .build();
        }

        Map<AgentReplyResponse, Integer> stanceScores = agentReplies.stream()
                .collect(Collectors.toMap(reply -> reply, reply -> stanceScore(reply.getContent())));

        boolean hasPositive = stanceScores.values().stream().anyMatch(score -> score > 0);
        boolean hasNegative = stanceScores.values().stream().anyMatch(score -> score < 0);
        boolean conflictDetected = hasPositive && hasNegative;

        if (!conflictDetected) {
            double minSimilarity = pairwiseSimilarity(agentReplies);
            conflictDetected = minSimilarity < 0.35;
        }

        if (!conflictDetected) {
            return ConflictInsightResponse.builder()
                    .conflictDetected(false)
                    .divergencePoints(List.of())
                    .verificationPoints(List.of())
                    .conflictingAgents(List.of())
                    .build();
        }

        List<String> conflictingAgents = stanceScores.entrySet().stream()
                .filter(entry -> Math.abs(entry.getValue()) > 0)
                .map(entry -> entry.getKey().getTitle())
                .distinct()
                .toList();

        List<String> divergencePoints = new ArrayList<>();
        divergencePoints.add("Agents differ on the interpretation of: " + query);
        divergencePoints.add("Some agents favor a conservative reading while others favor a broader one.");

        List<String> verificationPoints = new ArrayList<>();
        verificationPoints.add("Verify the disputed claim against authoritative source documents.");
        verificationPoints.add("Check whether the conflicting point is due to missing or outdated evidence.");

        return ConflictInsightResponse.builder()
                .conflictDetected(true)
                .divergencePoints(divergencePoints)
                .verificationPoints(verificationPoints)
                .conflictingAgents(conflictingAgents)
                .build();
    }

    private ConfidenceAssessmentResponse assessConfidence(
            List<RagRetrievalResponse> retrievals,
            String webSearchResult,
            List<AgentReplyResponse> agentReplies
    ) {
        double coverageRate = calculateCoverageRate(retrievals, agentReplies);
        double freshnessRate = calculateFreshnessRate(retrievals, webSearchResult);
        double consistencyRate = calculateConsistencyRate(agentReplies);
        double score = Math.round(100.0 * (
                0.40 * coverageRate
                        + 0.25 * freshnessRate
                        + 0.35 * consistencyRate
        ));

        String level = score >= 80 ? "high" : score >= 60 ? "medium" : "low";

        return ConfidenceAssessmentResponse.builder()
                .score(score)
                .coverageRate(coverageRate)
                .freshnessRate(freshnessRate)
                .consistencyRate(consistencyRate)
                .level(level)
                .build();
    }

    private double calculateCoverageRate(List<RagRetrievalResponse> retrievals, List<AgentReplyResponse> agentReplies) {
        if (CollectionUtils.isEmpty(retrievals)) {
            long usedCount = agentReplies.stream()
                    .map(AgentReplyResponse::getReferences)
                    .filter(list -> list != null && !list.isEmpty())
                    .mapToLong(List::size)
                    .sum();
            return usedCount > 0 ? 0.55 : 0.20;
        }

        Set<String> usedReferences = new HashSet<>();
        for (AgentReplyResponse reply : agentReplies) {
            if (reply.getReferences() != null) {
                usedReferences.addAll(reply.getReferences());
            }
        }

        return clamp((double) usedReferences.size() / retrievals.size());
    }

    private double calculateFreshnessRate(List<RagRetrievalResponse> retrievals, String webSearchResult) {
        if (StringUtils.hasText(webSearchResult)) {
            return 0.80;
        }
        if (CollectionUtils.isEmpty(retrievals)) {
            return 0.45;
        }

        double total = 0.0;
        int count = 0;
        for (RagRetrievalResponse retrieval : retrievals) {
            total += freshnessFromMetadata(retrieval.getMetadata());
            count++;
        }
        return count == 0 ? 0.45 : clamp(total / count);
    }

    private double freshnessFromMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return 0.55;
        }

        List<String> keys = List.of("updatedAt", "updateTime", "publishedAt", "publishTime", "date", "year");
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value == null) {
                continue;
            }

            Double parsed = parseFreshnessValue(String.valueOf(value));
            if (parsed != null) {
                return parsed;
            }
        }

        return 0.55;
    }

    private Double parseFreshnessValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmed = value.trim();
        try {
            int year = Integer.parseInt(trimmed);
            int age = Math.max(0, Year.now().getValue() - year);
            return clamp(1.0 - (age * 0.15));
        } catch (NumberFormatException ignored) {
        }

        List<DateTimeFormatter> dateFormatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ISO_DATE_TIME
        );
        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                LocalDate date = LocalDate.parse(trimmed, formatter);
                long ageDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now()));
                return clamp(1.0 - Math.min(0.8, ageDays / 3650.0));
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(trimmed);
            long ageDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(dateTime.toLocalDate(), LocalDate.now()));
            return clamp(1.0 - Math.min(0.8, ageDays / 3650.0));
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(trimmed);
            long ageDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(dateTime.toLocalDate(), LocalDate.now()));
            return clamp(1.0 - Math.min(0.8, ageDays / 3650.0));
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private double calculateConsistencyRate(List<AgentReplyResponse> agentReplies) {
        if (agentReplies.size() < 2) {
            return 1.0;
        }

        double total = 0.0;
        int count = 0;
        for (int i = 0; i < agentReplies.size(); i++) {
            for (int j = i + 1; j < agentReplies.size(); j++) {
                total += jaccardSimilarity(agentReplies.get(i).getContent(), agentReplies.get(j).getContent());
                count++;
            }
        }
        return count == 0 ? 1.0 : clamp(total / count);
    }

    private double pairwiseSimilarity(List<AgentReplyResponse> agentReplies) {
        if (agentReplies.size() < 2) {
            return 1.0;
        }

        double min = 1.0;
        for (int i = 0; i < agentReplies.size(); i++) {
            for (int j = i + 1; j < agentReplies.size(); j++) {
                min = Math.min(min, jaccardSimilarity(agentReplies.get(i).getContent(), agentReplies.get(j).getContent()));
            }
        }
        return min;
    }

    private double jaccardSimilarity(String left, String right) {
        Set<String> leftSet = tokenSet(left);
        Set<String> rightSet = tokenSet(right);
        if (leftSet.isEmpty() || rightSet.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(leftSet);
        intersection.retainAll(rightSet);
        Set<String> union = new HashSet<>(leftSet);
        union.addAll(rightSet);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private Set<String> tokenSet(String text) {
        Set<String> tokens = new HashSet<>();
        if (!StringUtils.hasText(text)) {
            return tokens;
        }

        String normalized = text.toLowerCase().replaceAll("[\\p{P}\\p{S}\\s]+", "");
        for (int i = 0; i < normalized.length(); i++) {
            tokens.add(String.valueOf(normalized.charAt(i)));
        }
        return tokens;
    }

    private int stanceScore(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }

        String normalized = text.toLowerCase();
        int score = 0;

        for (String token : List.of("should", "recommend", "prefer", "better", "suggest", "can", "need", "must", "应该", "建议", "可以", "适合", "优先", "倾向")) {
            if (normalized.contains(token)) {
                score += 1;
            }
        }
        for (String token : List.of("should not", "not", "avoid", "cannot", "不建议", "不适合", "不应", "不能", "无法", "有待验证", "争议", "未必")) {
            if (normalized.contains(token)) {
                score -= 1;
            }
        }
        return score;
    }

    private String extractViewpoint(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        int index = trimmed.indexOf('\n');
        return index >= 0 ? trimmed.substring(0, index).trim() : trimmed;
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
