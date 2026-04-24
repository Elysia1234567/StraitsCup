package com.omnisource.draft.agent;

import com.omnisource.draft.llm.LlmClient;
import com.omnisource.draft.model.AgentRole;

public class HistorianAgent extends AbstractRoleAgent {

    public HistorianAgent(LlmClient llmClient) {
        super(llmClient);
    }

    @Override
    public AgentRole role() {
        return AgentRole.HISTORIAN;
    }

    @Override
    public String systemPrompt() {
        return PromptTemplates.HISTORIAN_PROMPT;
    }

    @Override
    protected String title() {
        return "历史学家视角";
    }
}
