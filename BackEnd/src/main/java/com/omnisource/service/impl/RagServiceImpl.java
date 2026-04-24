package com.omnisource.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.service.RagService;
import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAG 检索服务实现。
 */
@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_EMBEDDING = "embedding";

    private static final int TITLE_MAX_LENGTH = 512;
    private static final int CONTENT_MAX_LENGTH = 65535;
    private static final int SOURCE_MAX_LENGTH = 1024;

    private final ObjectMapper objectMapper;
    private final Path datasetPath;
    private final int defaultTopK;

    private final String collectionName;
    private final String milvusUri;
    private final String milvusDatabase;
    private final String milvusUsername;
    private final String milvusPassword;

    private final String qianwenApiKey;
    private final String embeddingEndpoint;
    private final String embeddingModel;

    private final List<KnowledgeDocument> documents = new ArrayList<>();
    private final RestTemplate restTemplate = new RestTemplate();

    private MilvusClient milvusClient;
    private boolean milvusReady;

    public RagServiceImpl(
            ObjectMapper objectMapper,
            @Value("${rag.dataset-path:Util/standardList.jsonl}") String datasetPath,
            @Value("${rag.default-top-k:3}") int defaultTopK,
            @Value("${rag.collection-name:omnisource_rag}") String collectionName,
            @Value("${milvus.host:127.0.0.1}") String milvusHost,
            @Value("${milvus.port:19530}") int milvusPort,
            @Value("${milvus.database:default}") String milvusDatabase,
            @Value("${milvus.username:}") String milvusUsername,
            @Value("${milvus.password:}") String milvusPassword,
            @Value("${qianwen.api-key:}") String qianwenApiKey,
            @Value("${qianwen.base-url:https://dashscope.aliyuncs.com/compatible-mode}") String qianwenBaseUrl,
            @Value("${qianwen.embedding-model:text-embedding-v3}") String embeddingModel) {
        this.objectMapper = objectMapper;
        this.datasetPath = resolveDatasetPath(datasetPath);
        this.defaultTopK = defaultTopK;
        this.collectionName = collectionName;
        this.milvusUri = buildMilvusUri(milvusHost, milvusPort);
        this.milvusDatabase = StringUtils.hasText(milvusDatabase) ? milvusDatabase : "default";
        this.milvusUsername = milvusUsername;
        this.milvusPassword = milvusPassword;
        this.qianwenApiKey = qianwenApiKey;
        this.embeddingEndpoint = resolveEmbeddingEndpoint(qianwenBaseUrl);
        this.embeddingModel = embeddingModel;

        initializeMilvusClient();
    }

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public synchronized void reload() {
        documents.clear();
        loadLocalDocuments();

        if (documents.isEmpty()) {
            milvusReady = false;
            log.warn("RAG 知识库为空，Milvus 同步跳过");
            return;
        }

        if (milvusClient == null) {
            initializeMilvusClient();
        }

        if (milvusClient == null) {
            milvusReady = false;
            log.warn("Milvus 客户端未初始化，当前使用本地兜底检索");
            return;
        }

        try {
            rebuildMilvusCollection();
            milvusReady = true;
        } catch (RuntimeException e) {
            milvusReady = false;
            log.warn("Milvus 同步失败，回退到本地检索: {}", e.getMessage(), e);
        }
    }

    @Override
    public synchronized List<RagRetrievalResponse> retrieve(String question, int topK) {
        int limit = Math.max(1, topK > 0 ? topK : defaultTopK);
        String normalizedQuestion = normalize(question);

        if (!StringUtils.hasText(normalizedQuestion) || documents.isEmpty()) {
            return List.of();
        }

        if (milvusReady && milvusClient != null) {
            try {
                List<RagRetrievalResponse> milvusResults = retrieveFromMilvus(question, limit);
                if (!milvusResults.isEmpty()) {
                    return milvusResults;
                }
            } catch (RuntimeException e) {
                log.warn("Milvus 检索失败，回退到本地检索: {}", e.getMessage());
            }
        }

        return documents.stream()
                .map(doc -> scoreDocument(doc, normalizedQuestion))
                .filter(result -> result.getScore() > 0)
                .sorted(Comparator.comparingDouble(RagRetrievalResponse::getScore).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized String buildContext(String question, int topK) {
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
    public synchronized boolean isReady() {
        return !documents.isEmpty();
    }

    private void initializeMilvusClient() {
        try {
            ConnectParam.Builder builder = ConnectParam.newBuilder()
                    .withUri(milvusUri)
                    .withDatabaseName(milvusDatabase);

            if (StringUtils.hasText(milvusUsername) && StringUtils.hasText(milvusPassword)) {
                String authorization = Base64.getEncoder()
                        .encodeToString((milvusUsername + ":" + milvusPassword).getBytes(StandardCharsets.UTF_8));
                builder.withAuthorization(authorization);
            }

            this.milvusClient = new MilvusServiceClient(builder.build());
            log.info("Milvus 客户端初始化完成: uri={}, db={}", milvusUri, milvusDatabase);
        } catch (RuntimeException e) {
            this.milvusClient = null;
            log.warn("Milvus 客户端初始化失败，将使用本地检索兜底: {}", e.getMessage(), e);
        }
    }

    private void rebuildMilvusCollection() {
        dropCollectionQuietly();

        List<KnowledgeDocument> snapshot = new ArrayList<>(documents);
        if (snapshot.isEmpty()) {
            return;
        }

        List<Float> firstEmbedding = embedText(buildEmbeddingText(snapshot.get(0)));
        if (firstEmbedding.isEmpty()) {
            throw new IllegalStateException("Embedding 返回为空，无法创建 Milvus 集合");
        }

        createMilvusCollection(firstEmbedding.size());
        insertDocuments(snapshot, firstEmbedding.size());

        milvusClient.flush(FlushParam.newBuilder()
                .withDatabaseName(milvusDatabase)
                .withCollectionNames(List.of(collectionName))
                .withSyncFlush(Boolean.TRUE)
                .build());

        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
            .withDatabaseName(milvusDatabase)
            .withCollectionName(collectionName)
            .build());

        log.info("Milvus 知识库同步完成: collection={}, rows={}", collectionName, snapshot.size());
    }

        private void createMilvusCollection(int dimension) {
        List<FieldType> fieldTypes = List.of(
            FieldType.newBuilder()
                .withName(FIELD_ID)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build(),
            FieldType.newBuilder()
                .withName(FIELD_TITLE)
                .withDataType(DataType.VarChar)
                .withMaxLength(TITLE_MAX_LENGTH)
                .build(),
            FieldType.newBuilder()
                .withName(FIELD_CONTENT)
                .withDataType(DataType.VarChar)
                .withMaxLength(CONTENT_MAX_LENGTH)
                .build(),
            FieldType.newBuilder()
                .withName(FIELD_SOURCE)
                .withDataType(DataType.VarChar)
                .withMaxLength(SOURCE_MAX_LENGTH)
                .build(),
            FieldType.newBuilder()
                .withName(FIELD_EMBEDDING)
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build());

        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
            .withCollectionName(collectionName)
            .withDatabaseName(milvusDatabase)
            .withDescription("OmniSource RAG knowledge base")
            .withShardsNum(1)
            .withFieldTypes(fieldTypes)
            .build();

        milvusClient.createCollection(createCollectionParam);

        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(milvusDatabase)
                .withFieldName(FIELD_EMBEDDING)
                .withIndexType(IndexType.AUTOINDEX)
                .withIndexName("embedding_index")
                .withMetricType(MetricType.COSINE)
                .build();

        milvusClient.createIndex(createIndexParam);
    }

    private void insertDocuments(List<KnowledgeDocument> docs, int dimension) {
        List<String> titles = new ArrayList<>(docs.size());
        List<String> contents = new ArrayList<>(docs.size());
        List<String> sources = new ArrayList<>(docs.size());
        List<List<Float>> embeddings = new ArrayList<>(docs.size());

        for (KnowledgeDocument doc : docs) {
            List<Float> embedding = embedText(buildEmbeddingText(doc));
            if (embedding.isEmpty()) {
                continue;
            }
            if (embedding.size() != dimension) {
                throw new IllegalStateException("Embedding 维度不一致");
            }

            titles.add(safeText(doc.title()));
            contents.add(safeText(doc.content()));
            sources.add(resolveSource(doc));
            embeddings.add(embedding);
        }

        if (embeddings.isEmpty()) {
            throw new IllegalStateException("没有可插入的文档向量");
        }

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(FIELD_TITLE, titles));
        fields.add(new InsertParam.Field(FIELD_CONTENT, contents));
        fields.add(new InsertParam.Field(FIELD_SOURCE, sources));
        fields.add(new InsertParam.Field(FIELD_EMBEDDING, embeddings));

        InsertParam insertParam = InsertParam.newBuilder()
                .withDatabaseName(milvusDatabase)
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        milvusClient.insert(insertParam);
    }

    private void dropCollectionQuietly() {
        try {
            milvusClient.dropCollection(DropCollectionParam.newBuilder()
                    .withDatabaseName(milvusDatabase)
                    .withCollectionName(collectionName)
                    .build());
        } catch (RuntimeException e) {
            log.debug("Milvus 集合不存在或无法删除，继续创建: {}", e.getMessage());
        }
    }

    private void loadLocalDocuments() {
        if (!Files.exists(datasetPath)) {
            log.warn("RAG 知识库文件不存在: {}", datasetPath.toAbsolutePath());
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

            log.info("RAG 知识库加载完成: {} 条, 文件: {}", documents.size(), datasetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("加载 RAG 知识库失败: {}", datasetPath.toAbsolutePath(), e);
        }
    }

    private List<RagRetrievalResponse> retrieveFromMilvus(String question, int topK) {
        List<Float> queryEmbedding = embedText(question);
        if (queryEmbedding.isEmpty()) {
            return List.of();
        }

        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                .withDatabaseName(milvusDatabase)
                .withCollectionName(collectionName)
                .build());

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withTopK(topK)
                .withVectors(List.of(queryEmbedding))
                .withVectorFieldName(FIELD_EMBEDDING)
                .withOutFields(List.of(FIELD_TITLE, FIELD_CONTENT, FIELD_SOURCE))
                .build();

        R<?> response = milvusClient.search(searchParam);
        if (response == null || response.getData() == null) {
            return List.of();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper((io.milvus.grpc.SearchResultData) response.getData());
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }

        List<RagRetrievalResponse> results = new ArrayList<>(scores.size());
        for (SearchResultsWrapper.IDScore score : scores) {
            Map<String, Object> fieldValues = score.getFieldValues();
            String title = safeText(asString(fieldValues.get(FIELD_TITLE)));
            String content = safeText(asString(fieldValues.get(FIELD_CONTENT)));
            String source = safeText(asString(fieldValues.get(FIELD_SOURCE)));

            Map<String, Object> metadata = new LinkedHashMap<>();
            if (StringUtils.hasText(source)) {
                metadata.put("source", source);
            }

            results.add(RagRetrievalResponse.builder()
                    .id(String.valueOf(score.getLongID()))
                    .title(title)
                    .score((double) score.getScore())
                    .content(content)
                    .metadata(metadata)
                    .build());
        }

        return results;
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

    private List<Float> embedText(String text) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(qianwenApiKey)) {
            return List.of();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(qianwenApiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", embeddingModel);
        body.put("input", text);

        try {
            String responseBody = restTemplate.postForObject(embeddingEndpoint, new HttpEntity<>(body, headers), String.class);
            if (!StringUtils.hasText(responseBody)) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode embeddingNode = extractEmbeddingNode(root);
            if (embeddingNode == null || !embeddingNode.isArray()) {
                log.warn("Embedding 响应格式不正确: {}", responseBody);
                return List.of();
            }

            List<Float> embedding = new ArrayList<>(embeddingNode.size());
            for (JsonNode value : embeddingNode) {
                embedding.add((float) value.asDouble());
            }

            return embedding;
        } catch (IOException | RestClientException e) {
            log.warn("Embedding 调用失败: {}", e.getMessage());
            return List.of();
        }
    }

    private JsonNode extractEmbeddingNode(JsonNode root) {
        if (root == null) {
            return null;
        }

        JsonNode dataNode = root.path("data");
        if (dataNode.isArray() && !dataNode.isEmpty()) {
            JsonNode first = dataNode.get(0);
            if (first != null) {
                JsonNode embedding = first.path("embedding");
                if (embedding.isArray()) {
                    return embedding;
                }
            }
        }

        JsonNode directEmbedding = root.path("embedding");
        if (directEmbedding.isArray()) {
            return directEmbedding;
        }

        return null;
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
                log.info("RAG 知识库路径已解析为: {}", normalized.toAbsolutePath());
                return normalized;
            }
        }

        Path fallback = Paths.get(configuredPath).normalize();
        log.warn("未找到可用的 RAG 知识库路径，回退到配置值: {}", fallback.toAbsolutePath());
        return fallback;
    }

    private String buildMilvusUri(String host, int port) {
        if (!StringUtils.hasText(host)) {
            return "http://127.0.0.1:" + port;
        }

        String normalized = host.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        return "http://" + normalized + ":" + port;
    }

    private String resolveEmbeddingEndpoint(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
        }

        String normalized = baseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/embeddings";
        }
        return normalized + "/v1/embeddings";
    }

    private String buildEmbeddingText(KnowledgeDocument doc) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(doc.title())) {
            builder.append(doc.title().trim()).append('\n');
        }
        if (StringUtils.hasText(doc.content())) {
            builder.append(doc.content().trim());
        }
        return builder.toString().trim();
    }

    private String resolveSource(KnowledgeDocument doc) {
        String source = safeText(asString(doc.metadata().get("source")));
        if (StringUtils.hasText(source)) {
            return source;
        }

        source = safeText(asString(doc.metadata().get("path")));
        if (StringUtils.hasText(source)) {
            return source;
        }

        return safeText(doc.id());
    }

    private record KnowledgeDocument(String id, String title, String content, Map<String, Object> metadata) {
    }
}