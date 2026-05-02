package com.omnisource.Agents;

import com.omnisource.dto.request.MultiAgentChatRequest;
import com.omnisource.dto.response.AgentReplyResponse;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.entity.Agent;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 器灵 Agent 提示词构建器。
 *
 * <p>功能：把 Agent 人设、用户问题、RAG 检索结果组装成大模型可理解的 Prompt。
 * 同时支持“无 RAG 沉浸式器灵”模式，适合轻芜这类只谈记忆和感受的角色。
 * 这里专门管理 Agent 的表达规则，业务服务只负责调用。</p>
 */
@Component
public class AgentPromptBuilder {

    private static final String NO_RAG_MARK = "NO_RAG";
    private static final String NO_RAG_CN_MARK = "无RAG";

    /**
     * 判断当前 Agent 是否需要跳过 RAG。
     *
     * @param agent 当前 Agent 配置
     * @return true 表示只使用人设模板和用户问题，不拼接知识库资料
     */
    public boolean shouldSkipRag(Agent agent) {
        return containsNoRagMark(agent.getPromptTemplate())
                || containsNoRagMark(agent.getConstraints())
                || containsNoRagMark(agent.getKnowledgeScope());
    }

    /**
     * 构建单个 Agent 的完整提示词。
     *
     * @param agent 当前要回答的器灵 Agent，提供名称、人设、语言风格、约束等配置
     * @param request 用户聊天请求，提供问题、会话 ID、文物主题和指定 Agent 编码
     * @param retrievals RAG 检索结果，作为 Agent 回答时必须优先参考的知识片段
     * @param webSearchResult 联网搜索结果，作为 RAG 不足时的补充资料
     * @return 已拼装好的完整 Prompt 文本
     */
    public String buildAgentPrompt(
            Agent agent,
            MultiAgentChatRequest request,
            List<RagRetrievalResponse> retrievals,
            String webSearchResult
    ) {
        if (shouldSkipRag(agent)) {
            return buildImmersivePrompt(agent, request);
        }

        String theme = StringUtils.hasText(request.getHeritageId()) ? request.getHeritageId() : "当前文物";
        String references = buildReferenceText(retrievals);
        String webReferences = buildWebSearchText(webSearchResult);

        return """
                你正在扮演一个“文物器灵 Agent”。请严格遵守下面的人设和资料边界。

                【Agent 人设】
                %s

                【性格】
                %s

                【知识范围】
                %s

                【语言风格】
                %s

                【约束】
                %s

                【当前文物/主题】
                %s

                【用户问题】
                %s

                【RAG 检索资料】
                %s

                【联网搜索补充资料】
                %s

                【输出要求】
                1. 用中文回答，保持“器灵”代入感。
                2. 资料优先级：Agent 人设约束 > RAG 检索资料 > 联网搜索补充资料 > 模型自身知识。
                3. RAG 与联网搜索冲突时，以 RAG 为准；资料不足时明确说“现有资料不足”。
                4. 每次回复不超过 %d 字。
                5. 结尾追加一行：参考片段：chunkId1, chunkId2。如果没有资料，写“参考片段：无”。
                """.formatted(
                agent.buildSystemPrompt(theme),
                nullToDefault(agent.getPersonality(), "沉稳、友善、尊重史实"),
                nullToDefault(agent.getKnowledgeScope(), "文物历史、工艺、审美与文化背景"),
                nullToDefault(agent.getLanguageStyle(), "清晰、温和、有画面感"),
                nullToDefault(agent.getConstraints(), "不编造史实，不夸大鉴定结论"),
                theme,
                request.getQuery(),
                references,
                webReferences,
                resolveMaxTokens(agent.getMaxTokens())
        );
    }

    /**
     * 构建无 RAG 的沉浸式器灵提示词。
     *
     * <p>适用场景：角色本身有完整人格模板，回答重点是“我记得什么、我感受到什么”，
     * 不希望模型解释年代、出土编号、博物馆信息等文物学知识。</p>
     *
     * @param agent 当前 Agent 配置，promptTemplate 中保存完整角色模板
     * @param request 用户聊天请求
     * @return 不包含 RAG 资料的沉浸式 Prompt
     */
    private String buildImmersivePrompt(Agent agent, MultiAgentChatRequest request) {
        String theme = StringUtils.hasText(request.getHeritageId()) ? request.getHeritageId() : "当前文物";

        return """
                你正在扮演一个沉浸式文物器灵 Agent。请完全进入角色，不要解释你是 AI，也不要跳出人设。

                【角色模板】
                %s

                【当前文物/主题】
                %s

                【用户问题】
                %s

                【额外约束】
                %s

                【输出要求】
                1. 严格按照角色模板回答。
                2. 不使用 RAG，不引用知识库，不输出“参考片段”。
                3. 不解释文物学知识，只谈记忆、感受、动作、情绪和自我叙述。
                4. 若角色模板中规定第一人称、字数、称谓、禁用词，必须优先遵守。
                """.formatted(
                agent.buildSystemPrompt(theme),
                theme,
                request.getQuery(),
                nullToDefault(agent.getConstraints(), "遵守角色模板中的所有约束")
        );
    }

    /**
     * 构建聚合回答文本。
     *
     * @param agentReplies 每个 Agent 的独立回复
     * @return 用 Agent 名称分段后的最终回复
     */
    public String buildFinalAnswer(List<AgentReplyResponse> agentReplies) {
        if (CollectionUtils.isEmpty(agentReplies)) {
            return "当前没有可用的器灵 Agent 回答。";
        }

        return agentReplies.stream()
                .map(reply -> "【" + reply.getTitle() + "】\n" + reply.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 把 RAG 检索结果转换为 Prompt 中的参考资料文本。
     *
     * @param retrievals RAG 返回的知识片段列表
     * @return 适合放入 Prompt 的资料文本
     */
    private String buildReferenceText(List<RagRetrievalResponse> retrievals) {
        if (CollectionUtils.isEmpty(retrievals)) {
            return "未检索到相关资料。";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < retrievals.size(); i++) {
            RagRetrievalResponse item = retrievals.get(i);
            builder.append("[")
                    .append(i + 1)
                    .append("] chunkId=")
                    .append(item.getId())
                    .append('\n')
                    .append("标题：")
                    .append(item.getTitle())
                    .append('\n')
                    .append("内容：")
                    .append(item.getContent())
                    .append('\n');

            if (item.getMetadata() != null && !item.getMetadata().isEmpty()) {
                builder.append("元数据：").append(item.getMetadata()).append('\n');
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    /**
     * 构建联网搜索补充资料文本。
     *
     * @param webSearchResult Tavily 等联网搜索服务返回的格式化结果
     * @return 可写入 Prompt 的联网资料；为空时返回明确占位文本
     */
    private String buildWebSearchText(String webSearchResult) {
        if (!StringUtils.hasText(webSearchResult)) {
            return "未启用联网搜索或未获得联网搜索结果。";
        }
        return webSearchResult.trim();
    }

    /**
     * 为空字段提供默认值。
     *
     * @param value 数据库中的配置值
     * @param defaultValue 当配置值为空时使用的默认内容
     * @return 可直接写入 Prompt 的文本
     */
    private String nullToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    /**
     * 判断文本中是否包含无 RAG 标记。
     *
     * @param value 待检查文本，通常来自 promptTemplate、constraints 或 knowledgeScope
     * @return true 表示包含 NO_RAG 或 无RAG 标记
     */
    private boolean containsNoRagMark(String value) {
        return StringUtils.hasText(value)
                && (value.contains(NO_RAG_MARK) || value.contains(NO_RAG_CN_MARK));
    }

    /**
     * 解析 Agent 最大回复字数。
     *
     * @param maxTokens 数据库中的 max_tokens 配置
     * @return 最终用于提示词约束的字数，上限为 500
     */
    private int resolveMaxTokens(Integer maxTokens) {
        if (maxTokens == null || maxTokens <= 0) {
            return 220;
        }
        return BigDecimal.valueOf(maxTokens).min(BigDecimal.valueOf(500)).intValue();
    }
}
