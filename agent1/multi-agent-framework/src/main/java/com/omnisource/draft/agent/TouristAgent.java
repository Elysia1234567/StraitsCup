package com.omnisource.draft.agent;

import com.omnisource.draft.llm.LlmClient;
import com.omnisource.draft.model.AgentRole;

public class TouristAgent extends AbstractRoleAgent {

    public TouristAgent(LlmClient llmClient) {
        super(llmClient);
    }

    @Override
    public AgentRole role() {
        return AgentRole.TOURIST;
    }

    @Override
    public String systemPrompt() {
        return PromptTemplates.TOURIST_PROMPT;
    }

    @Override
    protected String title() {
        return "游客视角";
    }
}
