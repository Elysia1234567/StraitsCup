package com.omnisource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个 Agent 的回答结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentReplyResponse {

    private String agentCode;
    private String title;
    private String content;
    private List<String> references;
    private Boolean searchUsed;
}
