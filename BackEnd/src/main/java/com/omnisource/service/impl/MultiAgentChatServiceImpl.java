package com.omnisource.service.impl;

import com.omnisource.dto.request.MultiAgentChatRequest;
import com.omnisource.dto.response.AgentReplyResponse;
import com.omnisource.dto.response.MultiAgentChatResponse;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.enums.AgentRole;
import com.omnisource.service.MultiAgentChatService;
import com.omnisource.service.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 多智能体聊天基础实现。
 */
@Service
public class MultiAgentChatServiceImpl implements MultiAgentChatService {

    private static final List<AgentRole> DEFAULT_ROLES = Arrays.asList(
            AgentRole.HISTORIAN,
            AgentRole.CRAFTSMAN,
            AgentRole.TOURIST
    );

    private final ChatClient chatClient;
    private final RagService ragService;
    private final Map<String, MultiAgentChatResponse> sessionStore = new ConcurrentHashMap<>();

    public MultiAgentChatServiceImpl(ChatModel chatModel, RagService ragService) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.ragService = ragService;
    }

    @Override
    public MultiAgentChatResponse chat(MultiAgentChatRequest request) {
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId().trim()
                : "sess_" + UUID.randomUUID();
        int topK = request.getTopK() != null && request.getTopK() > 0 ? request.getTopK() : 3;
        List<AgentRole> roles = CollectionUtils.isEmpty(request.getAgentRoles()) ? DEFAULT_ROLES : request.getAgentRoles();

        List<RagRetrievalResponse> retrievals = ragService.retrieve(request.getQuery(), topK);
        List<AgentReplyResponse> agentReplies = new ArrayList<>();

        for (AgentRole role : roles) {
            agentReplies.add(buildAgentReply(role, request, retrievals));
        }

        MultiAgentChatResponse response = MultiAgentChatResponse.builder()
                .sessionId(sessionId)
                .query(request.getQuery())
                .heritageId(request.getHeritageId())
                .finalAnswer(buildFinalAnswer(agentReplies))
                .agentReplies(agentReplies)
                .retrievals(retrievals)
                .build();

        sessionStore.put(sessionId, response);
        return response;
    }

    @Override
    public MultiAgentChatResponse getSessionResult(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        return sessionStore.get(sessionId.trim());
    }

    private AgentReplyResponse buildAgentReply(
            AgentRole role,
            MultiAgentChatRequest request,
            List<RagRetrievalResponse> retrievals
    ) {
        String content = chatClient.prompt()
                .user(buildRolePrompt(role, request, retrievals))
                .call()
                .content();

        return AgentReplyResponse.builder()
                .role(role)
                .title(role.getTitle())
                .content(content)
                .references(retrievals.stream().map(RagRetrievalResponse::getId).collect(Collectors.toList()))
                .build();
    }

    private String buildRolePrompt(
            AgentRole role,
            MultiAgentChatRequest request,
            List<RagRetrievalResponse> retrievals
    ) {
        String heritageHint = StringUtils.hasText(request.getHeritageId())
                ? "目标非遗项目 ID：" + request.getHeritageId()
                : "目标非遗项目 ID：未指定";

        String references = buildReferenceText(retrievals);

        return """
                你是一个非遗文化多智能体系统中的角色 Agent。
                你必须优先依据【检索资料】回答，不得编造事实；如果资料不足，请明确说明不确定。

                【角色要求】
                %s

                【任务信息】
                %s

                【用户问题】
                %s

                【检索资料】
                %s

                【输出要求】
                1. 只从当前角色视角回答。
                2. 优先使用检索资料中的信息。
                3. 回答结尾单独追加一行：参考片段：chunkId1, chunkId2
                4. 使用中文输出。
                """.formatted(roleInstruction(role), heritageHint, request.getQuery(), references);
    }

    private String roleInstruction(AgentRole role) {
        return switch (role) {
            case HISTORIAN -> """
                    你是“历史学家 Agent”。
                    重点解释历史来源、文化背景、发展脉络、地域传播和文化意义。
                    语言要严谨、克制、偏理性，不展开过细工艺教学。
                    """;
            case CRAFTSMAN -> """
                    你是“匠人 Agent”。
                    重点解释工艺步骤、材料、技法、关键工序和传承经验。
                    语言要自然、专业、易懂，强调“怎么做”和“为什么这样做”。
                    """;
            case TOURIST -> """
                    你是“游客 Agent”。
                    重点用通俗表达解释看点、体验感、理解门槛和大众价值。
                    语言要亲切、简洁、易懂，帮助普通用户快速理解。
                    """;
        };
    }

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

    private String buildFinalAnswer(List<AgentReplyResponse> agentReplies) {
        if (CollectionUtils.isEmpty(agentReplies)) {
            return "当前没有可用的 Agent 回答。";
        }

        return agentReplies.stream()
                .map(reply -> "【" + reply.getTitle() + "】\n" + reply.getContent())
                .collect(Collectors.joining("\n\n"));
    }
}
