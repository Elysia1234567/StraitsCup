package com.omnisource.draft.model;

import java.util.List;

public class AgentContext {

    private String sessionId;
    private String query;
    private String heritageId;
    private AgentRole role;
    private List<KnowledgeChunk> references;

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

    public AgentRole getRole() {
        return role;
    }

    public void setRole(AgentRole role) {
        this.role = role;
    }

    public List<KnowledgeChunk> getReferences() {
        return references;
    }

    public void setReferences(List<KnowledgeChunk> references) {
        this.references = references;
    }
}
