package com.omnisource.dto.response;

import java.util.Map;

/**
 * RAG检索结果响应。
 */
public class RagRetrievalResponse {

    private String id;
    private String title;
    private Double score;
    private String content;
    private Map<String, Object> metadata;

    public RagRetrievalResponse() {
    }

    public RagRetrievalResponse(String id, String title, Double score, String content, Map<String, Object> metadata) {
        this.id = id;
        this.title = title;
        this.score = score;
        this.content = content;
        this.metadata = metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static final class Builder {
        private String id;
        private String title;
        private Double score;
        private String content;
        private Map<String, Object> metadata;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public RagRetrievalResponse build() {
            return new RagRetrievalResponse(id, title, score, content, metadata);
        }
    }
}