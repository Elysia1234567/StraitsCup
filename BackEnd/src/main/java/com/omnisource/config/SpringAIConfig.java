package com.omnisource.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 * 配置千问 API（OpenAI 兼容模式）
 */
@Configuration
public class SpringAIConfig {

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    /**
     * 构造函数，注入配置属性
     * @param apiKey 千问 API Key
     * @param baseUrl 千问 API Base URL
     * @param model 使用的模型名称
     */
    public SpringAIConfig(
            @Value("${qianwen.api-key}") String apiKey,
            @Value("${qianwen.base-url}") String baseUrl,
            @Value("${qianwen.model}") String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    /**
     * 配置 OpenAI API 客户端，用于调用千问 API
     * @return OpenAiApi 实例
     */
    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(baseUrl, apiKey);
    }

    /**
     * 配置 ChatModel（同步调用）
     * @return OpenAiChatModel 实例
     */
    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
    }
}
