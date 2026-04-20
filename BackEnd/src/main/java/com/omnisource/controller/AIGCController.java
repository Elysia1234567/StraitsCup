package com.omnisource.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/aigc")
public class AIGCController {

    private final ChatClient chatClient;

    // 构造函数：我们将 Spring 帮我们配好的底层 ChatModel 注入进来
    // 然后用 builder 拼装出一个好用的 ChatClient 给后续的接口用
    public AIGCController(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 接口一：同步调用（一次性返回所有文字）
     * 访问如: GET /api/aigc/chat?message=长城在哪里
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "你好") String message) {
        return chatClient.prompt()           // 开始配置我们要对 AI 说的内容
                .user(message)               // 传入用户的问题
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
        return chatClient.prompt()
                .user(message)
                .stream()                    // 发起【流式】调用
                .content();                  // 提取出包裹在 Flux 容器里的流数据串
    }
}