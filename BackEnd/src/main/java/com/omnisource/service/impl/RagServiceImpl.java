package com.omnisource.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.service.RagService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAG检索服务实现。
 * <p>
 * 当前版本使用本地JSONL知识库做最小闭环验证，后续可以替换为 Milvus / Redis Stack / 向量数据库。
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    private final ObjectMapper objectMapper;
    private final Path datasetPath;
    private final int defaultTopK;

    private final List<KnowledgeDocument> documents = new ArrayList<>();

    public RagServiceImpl(
            ObjectMapper objectMapper,
            @Value("${rag.dataset-path:Util/standardList.jsonl}") String datasetPath,
            @Value("${rag.default-top-k:3}") int defaultTopK) {
        this.objectMapper = objectMapper;
        this.datasetPath = resolveDatasetPath(datasetPath);
        this.defaultTopK = defaultTopK;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public synchronized void reload() {
        documents.clear();

        if (!Files.exists(datasetPath)) {
            log.warn("RAG知识库文件不存在: {}", datasetPath.toAbsolutePath());
            log.warn("当前工作目录: {}", Paths.get(".").toAbsolutePath().normalize());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(datasetPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!StringUtils.hasText(line)) {
                    continue;
                }

                JsonNode node = objectMapper.readTree(line);
                String id = safeText(node.path("id").asText(null));
                String title = safeText(node.path("title").asText(null));
                String content = safeText(node.path("content").asText(null));

                Map<String, Object> metadata = new LinkedHashMap<>();
                JsonNode metadataNode = node.path("metadata");
                if (metadataNode != null && metadataNode.isObject()) {
                    metadata = objectMapper.convertValue(metadataNode, new TypeReference<Map<String, Object>>() {
                    });
                }

                if (!StringUtils.hasText(id)) {
                    id = title;
                }

                documents.add(new KnowledgeDocument(id, title, content, metadata));
            }
            log.info("RAG知识库加载完成: {} 条, 文件: {}", documents.size(), datasetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("加载RAG知识库失败: {}", datasetPath.toAbsolutePath(), e);
        }
    }

    @Override
    public List<RagRetrievalResponse> retrieve(String question, int topK) {
        int limit = Math.max(1, topK > 0 ? topK : defaultTopK);
        String normalizedQuestion = normalize(question);

        if (!StringUtils.hasText(normalizedQuestion) || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(doc -> scoreDocument(doc, normalizedQuestion))
                .filter(result -> result.getScore() > 0)
                .sorted(Comparator.comparingDouble(RagRetrievalResponse::getScore).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public String buildContext(String question, int topK) {
        List<RagRetrievalResponse> results = retrieve(question, topK);
        if (results.isEmpty()) {
            return "未检索到相关知识。";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("已检索到的相关知识：\n");
        for (int i = 0; i < results.size(); i++) {
            RagRetrievalResponse item = results.get(i);
            builder.append("\n[").append(i + 1).append("] ")
                    .append(item.getTitle())
                    .append(" (score=").append(String.format("%.2f", item.getScore())).append(")\n");

            Map<String, Object> metadata = item.getMetadata();
            if (metadata != null && !metadata.isEmpty()) {
                Object level = metadata.get("level");
                Object category = metadata.get("category");
                Object region = metadata.get("region");
                if (level != null) {
                    builder.append("- 非遗级别：").append(level).append('\n');
                }
                if (category != null) {
                    builder.append("- 类别：").append(category).append('\n');
                }
                if (region != null) {
                    builder.append("- 地区：").append(region).append('\n');
                }
            }

            builder.append("- 内容：").append(item.getContent()).append('\n');
        }

        return builder.toString();
    }

    @Override
    public boolean isReady() {
        return !documents.isEmpty();
    }

    private RagRetrievalResponse scoreDocument(KnowledgeDocument doc, String normalizedQuestion) {
        String normalizedTitle = normalize(doc.title());
        String normalizedContent = normalize(doc.content());
        String normalizedCategory = normalize(asString(doc.metadata().get("category")));
        String normalizedRegion = normalize(asString(doc.metadata().get("region")));
        String normalizedLevel = normalize(asString(doc.metadata().get("level")));
        String normalizedWorks = normalize(asString(doc.metadata().get("works")));

        double score = 0.0;

        if (StringUtils.hasText(normalizedTitle) && normalizedQuestion.contains(normalizedTitle)) {
            score += 80.0;
        }
        if (StringUtils.hasText(normalizedTitle) && normalizedTitle.contains(normalizedQuestion)) {
            score += 60.0;
        }
        if (StringUtils.hasText(normalizedContent) && normalizedContent.contains(normalizedQuestion)) {
            score += 45.0;
        }

        score += jaccard(normalizedQuestion, normalizedTitle) * 35.0;
        score += jaccard(normalizedQuestion, normalizedCategory) * 20.0;
        score += jaccard(normalizedQuestion, normalizedRegion) * 15.0;
        score += jaccard(normalizedQuestion, normalizedLevel) * 10.0;
        score += jaccard(normalizedQuestion, normalizedWorks) * 12.0;
        score += jaccard(normalizedQuestion, normalizedContent) * 18.0;

        if (containsAny(normalizedQuestion, normalizedCategory, normalizedLevel, normalizedRegion)) {
            score += 8.0;
        }

        return RagRetrievalResponse.builder()
                .id(doc.id())
                .title(doc.title())
                .score(score)
                .content(doc.content())
                .metadata(doc.metadata())
                .build();
    }

    private boolean containsAny(String target, String... terms) {
        for (String term : terms) {
            if (StringUtils.hasText(term) && target.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private double jaccard(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return 0.0;
        }

        Set<String> leftSet = toCharacterSet(left);
        Set<String> rightSet = toCharacterSet(right);
        if (leftSet.isEmpty() || rightSet.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(leftSet);
        intersection.retainAll(rightSet);

        Set<String> union = new HashSet<>(leftSet);
        union.addAll(rightSet);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private Set<String> toCharacterSet(String text) {
        Set<String> chars = new HashSet<>();
        String normalized = normalize(text);
        for (int i = 0; i < normalized.length(); i++) {
            chars.add(String.valueOf(normalized.charAt(i)));
        }
        return chars;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("[\\p{P}\\p{S}\\s]+", "")
                .trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Path resolveDatasetPath(String configuredPath) {
        List<Path> candidates = List.of(
                Paths.get(configuredPath),
                Paths.get("").resolve(configuredPath),
                Paths.get("").resolve("Util/standardList.jsonl"),
                Paths.get("").resolve("../Util/standardList.jsonl"),
                Paths.get("").resolve("BackEnd/../Util/standardList.jsonl")
        );

        for (Path candidate : candidates) {
            Path normalized = candidate.normalize();
            if (Files.exists(normalized)) {
                log.info("RAG知识库路径已解析为: {}", normalized.toAbsolutePath());
                return normalized;
            }
        }

        Path fallback = Paths.get(configuredPath).normalize();
        log.warn("未找到可用的RAG知识库路径，回退到配置值: {}", fallback.toAbsolutePath());
        return fallback;
    }

    private record KnowledgeDocument(String id, String title, String content, Map<String, Object> metadata) {
    }
}