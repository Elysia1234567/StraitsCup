package com.omnisource.service;

import com.omnisource.dto.request.MultiAgentChatRequest;
import com.omnisource.dto.response.MultiAgentChatResponse;

/**
 * 多智能体聊天服务。
 */
public interface MultiAgentChatService {

    /**
     * 执行一次多智能体对话。
     *
     * @param request 请求参数
     * @return 聚合结果
     */
    MultiAgentChatResponse chat(MultiAgentChatRequest request);

    /**
     * 获取某个会话最近一次结果。
     *
     * @param sessionId 会话 ID
     * @return 最近一次结果，不存在时返回 null
     */
    MultiAgentChatResponse getSessionResult(String sessionId);
}
