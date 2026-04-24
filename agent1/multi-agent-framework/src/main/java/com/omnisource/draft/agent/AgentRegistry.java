package com.omnisource.draft.agent;

import com.omnisource.draft.model.AgentRole;
import java.util.EnumMap;
import java.util.Map;

public class AgentRegistry {

    private final Map<AgentRole, AgentExecutor> agents = new EnumMap<>(AgentRole.class);

    public void register(AgentExecutor executor) {
        agents.put(executor.role(), executor);
    }

    public AgentExecutor get(AgentRole role) {
        return agents.get(role);
    }
}
