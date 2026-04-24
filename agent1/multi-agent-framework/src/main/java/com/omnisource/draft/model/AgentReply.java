package com.omnisource.draft.model;

import java.util.List;

public class AgentReply {

    private AgentRole role;
    private String title;
    private String content;
    private List<String> referenceIds;

    public AgentRole getRole() {
        return role;
    }

    public void setRole(AgentRole role) {
        this.role = role;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getReferenceIds() {
        return referenceIds;
    }

    public void setReferenceIds(List<String> referenceIds) {
        this.referenceIds = referenceIds;
    }
}
