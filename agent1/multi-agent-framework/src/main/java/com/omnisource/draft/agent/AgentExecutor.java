package com.omnisource.draft.agent;

import com.omnisource.draft.model.AgentContext;
import com.omnisource.draft.model.AgentReply;
import com.omnisource.draft.model.AgentRole;

public interface AgentExecutor {

    AgentRole role();

    String systemPrompt();

    AgentReply reply(AgentContext context);
}
