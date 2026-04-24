package com.omnisource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 多智能体聊天聚合结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiAgentChatResponse {

    private String sessionId;
    private String query;
    private String heritageId;
    private String finalAnswer;
    private List<AgentReplyResponse> agentReplies;
    private List<RagRetrievalResponse> retrievals;
}
