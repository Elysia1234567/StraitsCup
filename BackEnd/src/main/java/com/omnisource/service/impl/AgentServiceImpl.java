package com.omnisource.service.impl;

import com.omnisource.Agents.AgentDefinition;
import com.omnisource.Agents.AgentDefaults;
import com.omnisource.Agents.FujianCulturalAgentRegistry;
import com.omnisource.entity.Agent;
import com.omnisource.mapper.AgentMapper;
import com.omnisource.service.AgentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;

    @PostConstruct
    public void syncCityAgents() {
        FujianCulturalAgentRegistry.DEFINITIONS.forEach(this::upsertAgentDefinition);
        log.info("Synced {} Fujian city agents", FujianCulturalAgentRegistry.DEFINITIONS.size());
    }

    @Override
    public List<Agent> getAllActiveAgents() {
        List<Agent> agents = agentMapper.selectAllActive();
        if (!agents.isEmpty()) {
            return agents.stream()
                    .filter(agent -> AgentDefaults.ALL_CITY_AGENT_CODES.contains(agent.getAgentCode()))
                    .toList();
        }
        return List.of();
    }

    @Override
    public Agent getAgentByCode(String agentCode) {
        return agentMapper.selectByCode(agentCode);
    }

    @Override
    public Agent getAgentById(Long id) {
        return agentMapper.selectById(id);
    }

    private void upsertAgentDefinition(AgentDefinition definition) {
        Agent agent = agentMapper.selectAnyByCode(definition.getAgentCode());
        Agent synced = toAgent(definition);
        if (agent == null) {
            agentMapper.insert(synced);
            return;
        }
        synced.setId(agent.getId());
        agentMapper.update(synced);
    }

    private Agent toAgent(AgentDefinition definition) {
        Agent agent = new Agent();
        agent.setAgentCode(definition.getAgentCode());
        agent.setName(definition.getName());
        agent.setAvatar(definition.getAvatar());
        agent.setRoleType(definition.getRoleType());
        agent.setPersonality(definition.getPersonality());
        agent.setPromptTemplate(definition.getPromptTemplate());
        agent.setKnowledgeScope(definition.getKnowledgeScope());
        agent.setLanguageStyle(definition.getLanguageStyle());
        agent.setConstraints(definition.getConstraints());
        agent.setMaxTokens(definition.getMaxTokens());
        agent.setTemperature(definition.getTemperature());
        agent.setTopP(definition.getTopP());
        agent.setIsPreset(1);
        agent.setSortOrder(definition.getSortOrder());
        agent.setStatus(1);
        return agent;
    }
}
