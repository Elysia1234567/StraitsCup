package com.omnisource.draft.model;

import java.util.List;

public class ChatRequest {

    private String sessionId;
    private String query;
    private String heritageId;
    private Integer topK;
    private List<AgentRole> agentRoles;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getHeritageId() {
        return heritageId;
    }

    public void setHeritageId(String heritageId) {
        this.heritageId = heritageId;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public List<AgentRole> getAgentRoles() {
        return agentRoles;
    }

    public void setAgentRoles(List<AgentRole> agentRoles) {
        this.agentRoles = agentRoles;
    }
}
