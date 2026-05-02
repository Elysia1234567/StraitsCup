package com.omnisource.service.impl;

import com.omnisource.service.TavilySearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TavilySearchServiceImpl implements TavilySearchService {

    @Value("${tavily.api-key}")
    private String apiKey;

    @Value("${tavily.max-results:3}")
    private int maxResults;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TAVILY_API_URL = "https://api.tavily.com/search";

    @Override
    public String searchAndFormat(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("api_key", apiKey);
            requestBody.put("query", query);
            requestBody.put("search_depth", "basic");
            requestBody.put("max_results", maxResults);
            requestBody.put("include_answer", true);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(TAVILY_API_URL, request, Map.class);

            return formatResults(response);
        } catch (Exception e) {
            log.error("Tavily搜索失败", e);
            return "[搜索服务暂时不可用]";
        }
    }

    @SuppressWarnings("unchecked")
    private String formatResults(Map<String, Object> response) {
        if (response == null) {
            return "[无搜索结果]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【联网搜索结果】\n");

        Object answer = response.get("answer");
        if (answer != null) {
            sb.append("搜索摘要：").append(answer).append("\n");
        }

        Object results = response.get("results");
        if (results instanceof List) {
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) results;
            sb.append("相关来源：\n");
            int index = 1;
            for (Map<String, Object> result : resultList) {
                String title = (String) result.getOrDefault("title", "未知标题");
                String url = (String) result.getOrDefault("url", "");
                String content = (String) result.getOrDefault("content", "");
                if (content != null && content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                sb.append(index).append(". ").append(title).append("\n");
                sb.append("   ").append(content).append("\n");
                sb.append("   来源：").append(url).append("\n");
                index++;
            }
        }

        return sb.toString();
    }
}
