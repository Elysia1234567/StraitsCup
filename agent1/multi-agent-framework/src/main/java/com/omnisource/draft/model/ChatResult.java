package com.omnisource.draft.model;

import java.util.List;

public class ChatResult {

    private String sessionId;
    private String query;
    private String finalAnswer;
    private List<AgentReply> agentReplies;
    private List<KnowledgeChunk> retrievals;

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

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    public List<AgentReply> getAgentReplies() {
        return agentReplies;
    }

    public void setAgentReplies(List<AgentReply> agentReplies) {
        this.agentReplies = agentReplies;
    }

    public List<KnowledgeChunk> getRetrievals() {
        return retrievals;
    }

    public void setRetrievals(List<KnowledgeChunk> retrievals) {
        this.retrievals = retrievals;
    }
}
