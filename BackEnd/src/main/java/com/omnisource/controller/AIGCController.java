package com.omnisource.controller;

import com.omnisource.entity.Agent;
import com.omnisource.entity.ChatMessage;
import com.omnisource.entity.ImageGenerationTask;
import com.omnisource.service.AgentService;
import com.omnisource.service.ChatHistoryService;
import com.omnisource.service.ImageGenerationService;
import com.omnisource.service.MultimodalService;
import com.omnisource.service.RagService;
import com.omnisource.utils.Result;
import com.omnisource.websocket.WebSocketSessionManager;
import com.omnisource.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/aigc")
@RequiredArgsConstructor
public class AIGCController {

    private final ChatClient chatClient;
    private final MultimodalService multimodalService;
    private final ImageGenerationService imageGenerationService;
    private final RagService ragService;
    private final AgentService agentService;
    private final ChatHistoryService chatHistoryService;
    private final WebSocketSessionManager sessionManager;

    @Value("${aliyun.oss.public-base-url:https://java-ai-fzu.oss-cn-beijing.aliyuncs.com}")
    private String ossPublicBaseUrl;

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "你好") String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<String> streamChat(@RequestParam(value = "message", defaultValue = "你好，请自我介绍") String message) {
        String prompt = buildRagPrompt(message);
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    @PostMapping("/multimodal")
    public Result<String> multimodal(@RequestBody Map<String, String> request) {
        String imageUrl = request.get("imageUrl");
        String question = request.getOrDefault("question", "请描述这张图片");
        String result = multimodalService.analyzeImage(imageUrl, question);
        return Result.success(result);
    }

    @PostMapping("/image")
    public Result<Map<String, String>> generateImage(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : 1L;
        Long roomId = request.get("roomId") != null ? Long.valueOf(request.get("roomId").toString()) : null;
        String prompt = valueOf(request.get("prompt"));
        String style = valueOf(request.get("style"));
        String agentCode = valueOf(request.get("agentCode"));

        Agent agent = StringUtils.hasText(agentCode) ? agentService.getAgentByCode(agentCode) : null;
        String referenceImageUrl = agent == null ? null : sourceImageUrl(agent);
        String finalPrompt = agent == null ? prompt : buildAgentImagePrompt(agent, prompt, referenceImageUrl);

        String imageUrl = imageGenerationService.generateImageAndReturnUrl(
                userId,
                roomId,
                finalPrompt,
                style,
                referenceImageUrl
        );

        Long messageId = null;
        if (roomId != null && agent != null) {
            ChatMessage msg = saveImageMessage(roomId, agent, prompt, imageUrl);
            messageId = msg.getId();
            broadcastImageMessage(roomId, agent, msg);
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("imageUrl", imageUrl);
        if (agent != null) {
            data.put("agentCode", agent.getAgentCode());
            data.put("agentName", agent.getName());
            data.put("agentAvatar", agent.getAvatar());
            data.put("sourceImageUrl", referenceImageUrl);
        }
        if (messageId != null) {
            data.put("messageId", String.valueOf(messageId));
        }
        return Result.success(data);
    }

    @GetMapping("/image/tasks/{taskId}")
    public Result<ImageGenerationTask> getTaskStatus(@PathVariable String taskId) {
        return Result.success(imageGenerationService.getTask(taskId));
    }

    private ChatMessage saveImageMessage(Long roomId, Agent agent, String prompt, String imageUrl) {
        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setMessageType("IMAGE");
        msg.setSenderType("AGENT");
        msg.setSenderId(agent.getAgentCode());
        msg.setSenderName(agent.getName());
        msg.setSenderAvatar(agent.getAvatar());
        msg.setContent("我根据自己的器灵形象生成了这张图。" + (StringUtils.hasText(prompt) ? "\n" + prompt : ""));
        msg.setImageUrl(imageUrl);
        msg.setIsStream(0);
        msg.setSearchEnabled(0);
        msg.setCreateTime(LocalDateTime.now());
        chatHistoryService.addMessage(msg);
        return msg;
    }

    private void broadcastImageMessage(Long roomId, Agent agent, ChatMessage msg) {
        WebSocketMessage wsMsg = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.IMAGE)
                .senderType(WebSocketMessage.SenderType.AGENT)
                .senderId(agent.getAgentCode())
                .senderName(agent.getName())
                .senderAvatar(agent.getAvatar())
                .roomId(roomId)
                .messageId(msg.getId())
                .content(msg.getContent())
                .imageUrl(msg.getImageUrl())
                .timestamp(msg.getCreateTime())
                .build();
        sessionManager.broadcastToRoom(roomId, wsMsg);
    }

    private String buildAgentImagePrompt(Agent agent, String prompt, String sourceImageUrl) {
        return """
                Generate an image that matches this specific cultural spirit agent.
                Agent name: %s
                Agent code: %s
                Personality: %s
                Visual/avatar reference URL: %s
                User request: %s

                Keep the character identity consistent with the reference image and the agent personality.
                The result should be suitable for a chat room image message.
                """.formatted(
                agent.getName(),
                agent.getAgentCode(),
                nullToEmpty(agent.getPersonality()),
                nullToEmpty(sourceImageUrl),
                nullToEmpty(prompt)
        );
    }

    private String sourceImageUrl(Agent agent) {
        String filename = sourceImageFilename(agent.getAgentCode());
        if (!StringUtils.hasText(filename)) {
            return agent.getAvatar();
        }
        String city = cityName(agent.getAgentCode());
        if (!StringUtils.hasText(city)) {
            return agent.getAvatar();
        }
        return normalizeBaseUrl(ossPublicBaseUrl) + "/OmniSource/source/" + city + "/" + filename + ".png";
    }

    private String sourceImageFilename(String agentCode) {
        return switch (agentCode) {
            case "fz_shoushan_stone" -> "寿山石雕";
            case "fz_cork_scene" -> "福州软木画";
            case "fz_lacquerware" -> "脱胎漆器";
            case "xm_bead_embroidery" -> "厦门珠绣";
            case "xm_lacquer_thread" -> "漆线雕";
            case "xm_wangchuan" -> "送王船";
            case "qz_dehua_porcelain" -> "德化瓷";
            case "qz_paper_lantern" -> "刻纸花灯";
            case "qz_string_puppet" -> "提线木偶";
            case "zz_glove_puppet" -> "布袋木偶戏";
            case "zz_woodblock_print" -> "木版年画";
            case "zz_pien_tze_huang" -> "片仔癀";
            case "pt_puxian_opera" -> "莆仙戏";
            case "pt_silver_ornament" -> "莆田银饰";
            case "pt_longan_woodcarving" -> "龙眼木雕";
            case "np_jian_ware" -> "建阳建盏（黑釉瓷）";
            case "np_wuyi_tea" -> "武夷岩茶";
            case "np_nuo_mask" -> "邵武傩面具";
            case "sm_hakka_bamboo" -> "客家竹编";
            case "sm_danankeng_pottery" -> "将乐大南坑陶瓷";
            case "sm_mingxi_microcarving" -> "明溪微雕";
            case "ly_hakka_rice_wine" -> "客家米酒";
            case "ly_hakka_embroidery" -> "龙岩客家刺绣";
            case "ly_farmer_painting" -> "农民画";
            case "nd_zherong_papercut" -> "柘荣剪纸";
            case "nd_she_costume" -> "畲族服饰";
            case "nd_line_lion" -> "霍童线狮";
            default -> null;
        };
    }

    private String cityName(String agentCode) {
        if (agentCode == null || !agentCode.contains("_")) {
            return null;
        }
        return switch (agentCode.substring(0, agentCode.indexOf('_'))) {
            case "fz" -> "福州";
            case "xm" -> "厦门";
            case "qz" -> "泉州";
            case "zz" -> "漳州";
            case "pt" -> "莆田";
            case "np" -> "南平";
            case "sm" -> "三明";
            case "ly" -> "龙岩";
            case "nd" -> "宁德";
            default -> null;
        };
    }

    private String buildRagPrompt(String message) {
        String references = ragService.retrieve(message, 3).stream()
                .map(item -> "chunkId=" + item.getId() + "\n标题：" + item.getTitle() + "\n内容：" + item.getContent())
                .collect(Collectors.joining("\n\n"));
        if (references.isBlank()) {
            references = "未检索到相关资料。";
        }

        return """
                请基于 RAG 检索资料回答用户问题。
                如果资料不足，请明确说明，不要编造。
                【用户问题】
                %s

                【检索资料】
                %s
                """.formatted(message, references);
    }

    private String valueOf(Object value) {
        return value == null ? null : value.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
