package com.omnisource.draft.agent;

import com.omnisource.draft.model.AgentContext;
import com.omnisource.draft.model.AgentReply;
import com.omnisource.draft.model.AgentRole;
import com.omnisource.draft.model.ChatRequest;
import com.omnisource.draft.model.ChatResult;
import com.omnisource.draft.model.KnowledgeChunk;
import com.omnisource.draft.rag.RetrievalService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentOrchestrator {

    private final AgentRegistry agentRegistry;
    private final RetrievalService retrievalService;
    private final AnswerAggregator answerAggregator;

    public AgentOrchestrator(
            AgentRegistry agentRegistry,
            RetrievalService retrievalService,
            AnswerAggregator answerAggregator
    ) {
        this.agentRegistry = agentRegistry;
        this.retrievalService = retrievalService;
        this.answerAggregator = answerAggregator;
    }

    public ChatResult chat(ChatRequest request) {
        int topK = request.getTopK() == null ? 3 : request.getTopK();
        Map<String, Object> filters = new HashMap<>();
        filters.put("heritageId", request.getHeritageId());

        List<KnowledgeChunk> retrievals = retrievalService.search(request.getQuery(), topK, filters);
        List<AgentReply> replies = new ArrayList<>();

        for (AgentRole role : request.getAgentRoles()) {
            AgentExecutor executor = agentRegistry.get(role);
            if (executor == null) {
                continue;
            }

            AgentContext context = new AgentContext();
            context.setSessionId(request.getSessionId());
            context.setQuery(request.getQuery());
            context.setHeritageId(request.getHeritageId());
            context.setRole(role);
            context.setReferences(retrievals);

            replies.add(executor.reply(context));
        }

        ChatResult result = new ChatResult();
        result.setSessionId(request.getSessionId());
        result.setQuery(request.getQuery());
        result.setRetrievals(retrievals);
        result.setAgentReplies(replies);
        result.setFinalAnswer(answerAggregator.aggregate(replies));
        return result;
    }
}
