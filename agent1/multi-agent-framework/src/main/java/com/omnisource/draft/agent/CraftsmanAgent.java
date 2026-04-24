package com.omnisource.draft.agent;

import com.omnisource.draft.llm.LlmClient;
import com.omnisource.draft.model.AgentRole;

public class CraftsmanAgent extends AbstractRoleAgent {

    public CraftsmanAgent(LlmClient llmClient) {
        super(llmClient);
    }

    @Override
    public AgentRole role() {
        return AgentRole.CRAFTSMAN;
    }

    @Override
    public String systemPrompt() {
        return PromptTemplates.CRAFTSMAN_PROMPT;
    }

    @Override
    protected String title() {
        return "匠人视角";
    }
}
