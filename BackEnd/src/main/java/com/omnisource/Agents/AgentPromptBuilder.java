package com.omnisource.Agents;

import com.omnisource.dto.request.MultiAgentChatRequest;
import com.omnisource.dto.response.AgentReplyResponse;
import com.omnisource.dto.response.AgentTraceResponse;
import com.omnisource.dto.response.ConfidenceAssessmentResponse;
import com.omnisource.dto.response.ConflictInsightResponse;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.entity.Agent;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AgentPromptBuilder {

    private static final String NO_RAG_MARK = "NO_RAG";
    private static final String NO_RAG_CN_MARK = "NO_RAG_CN";

    public boolean shouldSkipRag(Agent agent) {
        return containsNoRagMark(agent.getPromptTemplate())
                || containsNoRagMark(agent.getConstraints())
                || containsNoRagMark(agent.getKnowledgeScope());
    }

    public String buildAgentPrompt(
            Agent agent,
            MultiAgentChatRequest request,
            List<RagRetrievalResponse> retrievals,
            String webSearchResult
    ) {
        if (shouldSkipRag(agent)) {
            return buildImmersivePrompt(agent, request);
        }

        String theme = StringUtils.hasText(request.getHeritageId()) ? request.getHeritageId() : "current heritage topic";
        String references = buildReferenceText(retrievals);
        String webReferences = buildWebSearchText(webSearchResult);

        return """
                You are acting as a cultural heritage expert agent.
                Respond in Chinese.

                [Agent profile]
                %s

                [Personality]
                %s

                [Knowledge scope]
                %s

                [Style]
                %s

                [Constraints]
                %s

                [Topic]
                %s

                [User question]
                %s

                [RAG references]
                %s

                [Web references]
                %s

                [Requirements]
                1. Answer in Chinese.
                2. Use the references first.
                3. If the references are insufficient, say so explicitly.
                4. Keep it concise and grounded.
                5. End with a short reference note like "参考片段: chunkId1, chunkId2".
                """.formatted(
                agent.buildSystemPrompt(theme),
                nullToDefault(agent.getPersonality(), "calm, friendly, and historically grounded"),
                nullToDefault(agent.getKnowledgeScope(), "heritage history, craft, aesthetics, and cultural background"),
                nullToDefault(agent.getLanguageStyle(), "clear, warm, and vivid"),
                nullToDefault(agent.getConstraints(), "do not fabricate facts; do not overstate conclusions"),
                theme,
                request.getQuery(),
                references,
                webReferences
        );
    }

    private String buildImmersivePrompt(Agent agent, MultiAgentChatRequest request) {
        String theme = StringUtils.hasText(request.getHeritageId()) ? request.getHeritageId() : "current heritage topic";

        return """
                You are a fully immersive cultural heritage agent.
                Answer in character and do not mention that you are an AI.

                [Agent profile]
                %s

                [Topic]
                %s

                [User question]
                %s

                [Constraints]
                %s
                """.formatted(
                agent.buildSystemPrompt(theme),
                theme,
                request.getQuery(),
                nullToDefault(agent.getConstraints(), "follow the role template strictly")
        );
    }

    public String buildFinalAnswer(List<AgentReplyResponse> agentReplies) {
        return buildFinalAnswer(agentReplies, null, null);
    }

    public String buildFinalAnswer(
            List<AgentReplyResponse> agentReplies,
            ConflictInsightResponse conflictInsight,
            ConfidenceAssessmentResponse confidenceAssessment
    ) {
        if (CollectionUtils.isEmpty(agentReplies)) {
            return "No available agent response.";
        }

        StringBuilder builder = new StringBuilder();
        for (AgentReplyResponse reply : agentReplies) {
            builder.append("[").append(reply.getTitle()).append("]\n")
                    .append(nullToDefault(reply.getContent(), "")).append('\n');

            List<RagRetrievalResponse> evidence = reply.getEvidenceChunks();
            if (!CollectionUtils.isEmpty(evidence)) {
                builder.append("Evidence chain:\n");
                for (RagRetrievalResponse item : evidence) {
                    builder.append("- ")
                            .append(nullToDefault(item.getTitle(), "chunk"))
                            .append(" (").append(nullToDefault(item.getId(), "unknown")).append("): ")
                            .append(nullToDefault(item.getContent(), ""))
                            .append('\n');
                }
            }
            builder.append('\n');
        }

        if (conflictInsight != null && conflictInsight.isConflictDetected()) {
            builder.append("Conflict insight:\n");
            if (!CollectionUtils.isEmpty(conflictInsight.getDivergencePoints())) {
                for (String point : conflictInsight.getDivergencePoints()) {
                    builder.append("- ").append(point).append('\n');
                }
            }
            if (!CollectionUtils.isEmpty(conflictInsight.getVerificationPoints())) {
                builder.append("Verification points:\n");
                for (String point : conflictInsight.getVerificationPoints()) {
                    builder.append("- ").append(point).append('\n');
                }
            }
            builder.append('\n');
        }

        if (confidenceAssessment != null) {
            builder.append("Confidence: ")
                    .append(String.format("%.0f", confidenceAssessment.getScore()))
                    .append("/100 (coverage=")
                    .append(String.format("%.2f", confidenceAssessment.getCoverageRate()))
                    .append(", freshness=")
                    .append(String.format("%.2f", confidenceAssessment.getFreshnessRate()))
                    .append(", consistency=")
                    .append(String.format("%.2f", confidenceAssessment.getConsistencyRate()))
                    .append(")\n");
        }

        return builder.toString().trim();
    }

    private String buildReferenceText(List<RagRetrievalResponse> retrievals) {
        if (CollectionUtils.isEmpty(retrievals)) {
            return "No related retrievals.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < retrievals.size(); i++) {
            RagRetrievalResponse item = retrievals.get(i);
            builder.append("[")
                    .append(i + 1)
                    .append("] chunkId=")
                    .append(item.getId())
                    .append('\n')
                    .append("title: ")
                    .append(item.getTitle())
                    .append('\n')
                    .append("content: ")
                    .append(item.getContent())
                    .append('\n');

            if (item.getMetadata() != null && !item.getMetadata().isEmpty()) {
                builder.append("metadata: ").append(item.getMetadata()).append('\n');
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    private String buildWebSearchText(String webSearchResult) {
        if (!StringUtils.hasText(webSearchResult)) {
            return "No web result.";
        }
        return webSearchResult.trim();
    }

    private String nullToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private boolean containsNoRagMark(String value) {
        return StringUtils.hasText(value)
                && (value.contains(NO_RAG_MARK) || value.contains(NO_RAG_CN_MARK));
    }

    private int resolveMaxTokens(Integer maxTokens) {
        if (maxTokens == null || maxTokens <= 0) {
            return 220;
        }
        return BigDecimal.valueOf(maxTokens).min(BigDecimal.valueOf(500)).intValue();
    }
}
