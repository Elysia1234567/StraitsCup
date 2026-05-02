package com.omnisource.service.impl;

import com.omnisource.Agents.AgentDefaults;
import com.omnisource.Agents.AgentPromptBuilder;
import com.omnisource.dto.request.MultiAgentChatRequest;
import com.omnisource.dto.response.AgentReplyResponse;
import com.omnisource.dto.response.MultiAgentChatResponse;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.entity.Agent;
import com.omnisource.service.AgentService;
import com.omnisource.service.MultiAgentChatService;
import com.omnisource.service.RagService;
import com.omnisource.service.TavilySearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据库驱动的多器灵聊天实现。
 */
@Service
public class MultiAgentChatServiceImpl implements MultiAgentChatService {

    private final ChatClient chatClient;
    private final RagService ragService;
    private final AgentService agentService;
    private final TavilySearchService tavilySearchService;
    private final AgentPromptBuilder agentPromptBuilder;
    private final Map<String, MultiAgentChatResponse> sessionStore = new ConcurrentHashMap<>();

    /**
     * 创建多器灵聊天服务。
     *
     * @param chatModel Spring AI 聊天模型，用于调用大模型生成 Agent 回复
     * @param ragService RAG 检索服务，用于根据用户问题查询知识库资料
     * @param agentService Agent 数据服务，用于从数据库读取 Agent 配置
     * @param tavilySearchService 联网搜索服务，用于在用户允许时补充互联网资料
     * @param agentPromptBuilder Agent 提示词构建器，用于拼装人设、问题和 RAG 资料
     */
    public MultiAgentChatServiceImpl(
            ChatModel chatModel,
            RagService ragService,
            AgentService agentService,
            TavilySearchService tavilySearchService,
            AgentPromptBuilder agentPromptBuilder) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.ragService = ragService;
        this.agentService = agentService;
        this.tavilySearchService = tavilySearchService;
        this.agentPromptBuilder = agentPromptBuilder;
    }

    /**
     * 执行一次多 Agent 问答。
     *
     * @param request 用户请求，包含问题、文物主题、检索数量和指定 Agent 编码
     * @return 多个 Agent 的独立回复、聚合回复和 RAG 检索结果
     */
    @Override
    public MultiAgentChatResponse chat(MultiAgentChatRequest request) {
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId().trim()
                : "sess_" + UUID.randomUUID();
        int topK = request.getTopK() != null && request.getTopK() > 0 ? request.getTopK() : 3;

        List<Agent> agents = resolveAgents(request.getAgentCodes());
        List<RagRetrievalResponse> retrievals = needsRag(agents) ? ragService.retrieve(request.getQuery(), topK) : List.of();
        String webSearchResult = shouldUseWebSearch(request, agents)
                ? tavilySearchService.searchAndFormat(request.getQuery())
                : null;
        List<AgentReplyResponse> agentReplies = new ArrayList<>();

        for (Agent agent : agents) {
            agentReplies.add(buildAgentReply(agent, request, retrievals, webSearchResult));
        }

        MultiAgentChatResponse response = MultiAgentChatResponse.builder()
                .sessionId(sessionId)
                .query(request.getQuery())
                .heritageId(request.getHeritageId())
                .finalAnswer(agentPromptBuilder.buildFinalAnswer(agentReplies))
                .agentReplies(agentReplies)
                .retrievals(retrievals)
                .webSearchResult(webSearchResult)
                .build();

        sessionStore.put(sessionId, response);
        return response;
    }

    /**
     * 查询会话最近一次问答结果。
     *
     * @param sessionId 会话 ID，由 chat 方法自动生成或由前端传入
     * @return 会话最近一次结果，不存在时返回 null
     */
    @Override
    public MultiAgentChatResponse getSessionResult(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        return sessionStore.get(sessionId.trim());
    }

    /**
     * 解析本轮要参与回答的 Agent。
     *
     * @param requestedCodes 前端指定的 Agent 编码列表，为空时使用默认器灵
     * @return 可用的 Agent 配置列表
     */
    private List<Agent> resolveAgents(List<String> requestedCodes) {
        List<String> codes = CollectionUtils.isEmpty(requestedCodes) ? AgentDefaults.DEFAULT_AGENT_CODES : requestedCodes;
        LinkedHashSet<String> uniqueCodes = codes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Agent> agents = uniqueCodes.stream()
                .map(agentService::getAgentByCode)
                .filter(agent -> agent != null && (agent.getStatus() == null || agent.getStatus() == 1))
                .toList();

        if (!agents.isEmpty()) {
            return agents;
        }
        return agentService.getAllActiveAgents().stream().limit(2).toList();
    }

    /**
     * 判断本轮是否至少有一个 Agent 需要 RAG。
     *
     * @param agents 本轮参与回答的 Agent 列表
     * @return true 表示需要执行 RAG 检索，false 表示可以完全跳过检索
     */
    private boolean needsRag(List<Agent> agents) {
        return agents.stream().anyMatch(agent -> !agentPromptBuilder.shouldSkipRag(agent));
    }

    /**
     * 判断本轮是否需要联网搜索。
     *
     * @param request 用户聊天请求，searchEnabled 为 true 时才允许联网
     * @param agents 本轮参与回答的 Agent 列表
     * @return true 表示执行联网搜索；NO_RAG Agent 不会触发联网搜索
     */
    private boolean shouldUseWebSearch(MultiAgentChatRequest request, List<Agent> agents) {
        return Boolean.TRUE.equals(request.getSearchEnabled()) && needsRag(agents);
    }

    /**
     * 构建并调用单个 Agent 的回复。
     *
     * @param agent 当前要回答的 Agent 配置
     * @param request 用户聊天请求
     * @param retrievals 本轮 RAG 检索资料
     * @param webSearchResult 本轮联网搜索补充资料
     * @return 单个 Agent 的回复结果
     */
    private AgentReplyResponse buildAgentReply(
            Agent agent,
            MultiAgentChatRequest request,
            List<RagRetrievalResponse> retrievals,
            String webSearchResult
    ) {
        boolean skipRag = agentPromptBuilder.shouldSkipRag(agent);
        List<RagRetrievalResponse> agentRetrievals = skipRag ? List.of() : retrievals;
        String agentWebSearchResult = skipRag ? null : webSearchResult;

        String content = chatClient.prompt()
                .user(agentPromptBuilder.buildAgentPrompt(agent, request, agentRetrievals, agentWebSearchResult))
                .call()
                .content();

        return AgentReplyResponse.builder()
                .agentCode(agent.getAgentCode())
                .title(agent.getName())
                .content(content)
                .references(agentRetrievals.stream().map(RagRetrievalResponse::getId).collect(Collectors.toList()))
                .searchUsed(StringUtils.hasText(agentWebSearchResult))
                .build();
    }
}
