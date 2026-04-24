package com.omnisource.controller;

import com.omnisource.dto.request.MultiAgentChatRequest;
import com.omnisource.dto.response.MultiAgentChatResponse;
import com.omnisource.service.MultiAgentChatService;
import com.omnisource.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多智能体聊天接口。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MultiAgentChatService multiAgentChatService;

    /**
     * 发起一次多智能体问答。
     */
    @PostMapping
    public Result<MultiAgentChatResponse> chat(@Valid @RequestBody MultiAgentChatRequest request) {
        MultiAgentChatResponse response = multiAgentChatService.chat(request);
        return Result.success("多智能体问答成功", response);
    }

    /**
     * 查询会话最近一次结果。
     */
    @GetMapping("/{sessionId}")
    public Result<MultiAgentChatResponse> getSession(@PathVariable("sessionId") String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Result.badRequest("sessionId 不能为空");
        }

        MultiAgentChatResponse response = multiAgentChatService.getSessionResult(sessionId);
        if (response == null) {
            return Result.notFound("未找到对应会话结果");
        }

        return Result.success(response);
    }
}
