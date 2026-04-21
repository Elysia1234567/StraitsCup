package com.omnisource.controller;

import com.omnisource.service.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Objects;

@RestController
@RequestMapping("/api/aigc")
public class AIGCController {

    private final ChatClient chatClient;
    private final RagService ragService;

    // 构造函数：我们将 Spring 帮我们配好的底层 ChatModel 注入进来
    // 然后用 builder 拼装出一个好用的 ChatClient 给后续的接口用
    public AIGCController(ChatModel chatModel, RagService ragService) {
        this.chatClient = ChatClient.builder(Objects.requireNonNull(chatModel, "chatModel")).build();
        this.ragService = Objects.requireNonNull(ragService, "ragService");
    }

    /**
     * 接口一：同步调用（一次性返回所有文字）
     * 访问如: GET /api/aigc/chat?message=长城在哪里
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "你好") String message) {
        String prompt = Objects.requireNonNull(buildRagPrompt(message), "prompt");
        return chatClient.prompt()           // 开始配置我们要对 AI 说的内容
                .user(prompt)                // 传入包含RAG上下文的提示词
                .call()                      // 发起【同步】调用
                .content();                  // 提取 AI 返回的内容字符串
    }

    /**
     * 接口二：流式调用（字是一个个出来的）
     * 访问如: GET /api/aigc/stream?message=写一篇关于非遗的50字短文
     * 注意这里指定了返回的 MediaType 是 text/event-stream，这告诉浏览器我们要下发数据流。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<String> streamChat(@RequestParam(value = "message", defaultValue = "你好，请自我介绍") String message) {
        String prompt = Objects.requireNonNull(buildRagPrompt(message), "prompt");
        return chatClient.prompt()
                .user(prompt)
                .stream()                    // 发起【流式】调用
                .content();                  // 提取出包裹在 Flux 容器里的流数据串
    }

        /**
         * 组装RAG增强提示词。
         *
         * @param question 用户问题
         * @return 提示词
         */
        private String buildRagPrompt(String question) {
            String ragContext = ragService.buildContext(question, 0);
            return """
                你是一个面向非遗文化的数字传承人。
                请优先依据【检索到的知识】回答；如果知识不足，请明确说明不确定，不要编造。

                【检索到的知识】
                %s

                【用户问题】
                %s

                【回答要求】
                1. 回答要准确、简洁、中文表达自然。
                2. 如果涉及史实或来源，尽量结合检索内容说明。
                3. 不要输出与问题无关的内容。
                """.formatted(ragContext, question);
        }
}