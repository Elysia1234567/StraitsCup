package com.omnisource.dto.request;

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

    /**
     * 是否启用联网搜索。
     * true 表示 RAG 资料不足时允许引入互联网搜索结果；NO_RAG Agent 会自动忽略该开关。
     */
    private Boolean searchEnabled;

    /**
     * 要参与回答的 Agent 编码，例如 fz_shoushan_stone、xm_bead_embroidery。
     * 为空时使用系统默认城市 Agent。
     */
    private List<String> agentCodes;
}
