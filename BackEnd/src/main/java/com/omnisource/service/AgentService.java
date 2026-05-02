package com.omnisource.service;

import com.omnisource.entity.Agent;

import java.util.List;

public interface AgentService {
    List<Agent> getAllActiveAgents();
    Agent getAgentByCode(String agentCode);
    Agent getAgentById(Long id);
}
