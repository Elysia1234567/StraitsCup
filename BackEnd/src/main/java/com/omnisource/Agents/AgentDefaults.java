package com.omnisource.Agents;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default Agent configuration.
 */
public final class AgentDefaults {

    private static final int DEFAULT_MAX_AGENTS = 6;

    /**
     * Default chat room agents: pick one agent per Fujian city, up to 6.
     */
    public static final List<String> DEFAULT_AGENT_CODES = FujianCulturalAgentRegistry.DEFINITIONS.stream()
            .collect(Collectors.toMap(
                    definition -> cityPrefix(definition.getAgentCode()),
                    AgentDefinition::getAgentCode,
                    (first, ignored) -> first,
                    LinkedHashMap::new
            ))
            .values()
            .stream()
            .limit(DEFAULT_MAX_AGENTS)
            .toList();

    public static final List<String> ALL_CITY_AGENT_CODES = FujianCulturalAgentRegistry.DEFINITIONS.stream()
            .map(AgentDefinition::getAgentCode)
            .toList();

    private static String cityPrefix(String agentCode) {
        int separator = agentCode.indexOf('_');
        return separator > 0 ? agentCode.substring(0, separator) : agentCode;
    }

    private AgentDefaults() {
    }
}
