package com.omnisource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTraceResponse {

    private String agentCode;
    private String title;
    private List<RagRetrievalResponse> evidenceChunks;
    private String viewpoint;
    private String conclusion;
}
