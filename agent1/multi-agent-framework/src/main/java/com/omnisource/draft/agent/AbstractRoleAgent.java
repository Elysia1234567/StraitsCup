package com.omnisource.draft.agent;

import com.omnisource.draft.llm.LlmClient;
import com.omnisource.draft.model.AgentContext;
import com.omnisource.draft.model.AgentReply;
import com.omnisource.draft.model.KnowledgeChunk;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractRoleAgent implements AgentExecutor {

    private final LlmClient llmClient;

    protected AbstractRoleAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentReply reply(AgentContext context) {
        List<String> references = context.getReferences().stream()
                .map(KnowledgeChunk::getContent)
                .collect(Collectors.toList());

        String content = llmClient.chat(systemPrompt(), context.getQuery(), references);

        AgentReply reply = new AgentReply();
        reply.setRole(role());
        reply.setTitle(title());
        reply.setContent(content);
        reply.setReferenceIds(context.getReferences().stream()
                .map(KnowledgeChunk::getChunkId)
                .collect(Collectors.toList()));
        return reply;
    }

    protected abstract String title();
}
