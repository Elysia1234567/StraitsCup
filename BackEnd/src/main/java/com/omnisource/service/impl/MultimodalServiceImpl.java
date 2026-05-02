package com.omnisource.service.impl;

import com.omnisource.service.MultimodalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MultimodalServiceImpl implements MultimodalService {

    @Value("${qianwen.api-key}")
    private String apiKey;

    @Value("${qianwen.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String analyzeImage(String imageUrl, String question) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", "qwen-vl-plus",
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "image_url",
                                            "image_url", Map.of("url", imageUrl)),
                                    Map.of("type", "text", "text", question)
                            )
                    ))
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = baseUrl + "/v1/chat/completions";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            return extractContent(response);
        } catch (Exception e) {
            log.error("图片分析失败: url={}", imageUrl, e);
            return "[图片分析失败，请检查图片链接是否有效]";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) return "[无返回内容]";
        Object choices = response.get("choices");
        if (choices instanceof List) {
            List<Map<String, Object>> choiceList = (List<Map<String, Object>>) choices;
            if (!choiceList.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choiceList.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }
        return "[解析失败]";
    }
}
