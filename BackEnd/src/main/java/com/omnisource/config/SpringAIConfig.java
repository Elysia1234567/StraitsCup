package com.omnisource.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SpringAIConfig {

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public SpringAIConfig(
            @Value("${qianwen.api-key}") String apiKey,
            @Value("${qianwen.base-url}") String baseUrl,
            @Value("${qianwen.model}") String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    @Primary
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

    @Bean
    public OpenAiChatModel visionChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen-vl-plus")
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean
    public ChatClient visionChatClient(OpenAiChatModel visionChatModel) {
        return ChatClient.builder(visionChatModel).build();
    }
}
