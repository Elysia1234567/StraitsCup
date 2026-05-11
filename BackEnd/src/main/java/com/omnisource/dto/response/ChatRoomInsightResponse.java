package com.omnisource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomInsightResponse {

    private Long roomId;
    private String roomName;
    private Integer agentCount;
    private Integer messageCount;
    private String latestQuestion;
    private String latestAnswer;
    private String latestAgentName;
    private LocalDateTime latestUpdateTime;
    private String summary;
    private ConfidenceView confidence;
    private List<EvidenceSourceView> evidenceSources;
    private List<String> knowledgeTags;
    private List<RelationPathView> relationPaths;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfidenceView {
        private Double score;
        private String level;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceSourceView {
        private String id;
        private String title;
        private String provider;
        private Double confidence;
        private String date;
        private String excerpt;
        private String type;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationPathView {
        private String key;
        private String value;
    }
}
