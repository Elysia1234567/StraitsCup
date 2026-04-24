package com.omnisource.dto.request;

import com.omnisource.enums.AgentRole;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 多智能体聊天请求。
 */
@Data
public class MultiAgentChatRequest {

    private String sessionId;

    @NotBlank(message = "query 不能为空")
    private String query;

    private String heritageId;

    private Integer topK;

    private List<AgentRole> agentRoles;
}
