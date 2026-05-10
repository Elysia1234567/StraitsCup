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
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    private static final String FIELD_ID = "id";
    private static final String FIELD_DOC_KEY = "doc_key";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_EMBEDDING = "embedding";
    private static final String REDIS_HASH_PREFIX = "rag:sync:fingerprint:";
    private static final String REDIS_SCHEMA_VERSION_KEY = "rag:milvus:schema:version:";
    private static final String CURRENT_SCHEMA_VERSION = "3";

    private static final int TITLE_MAX_LENGTH = 512;
    private static final int CONTENT_MAX_LENGTH = 65535;
    private static final int SOURCE_MAX_LENGTH = 1024;
    private static final int DOC_KEY_MAX_LENGTH = 1024;
    private static final int METADATA_MAX_LENGTH = 8192;
    private static final int INSERT_BATCH_SIZE = 50;

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Path datasetPath;
    private final int defaultTopK;
    private final String collectionName;
    private final String milvusHost;
    private final int milvusPort;
    private final String milvusDatabase;
    private final String milvusUsername;
    private final String milvusPassword;
    private final boolean milvusEnabled;
    private final String qianwenApiKey;
    private final String embeddingEndpoint;
    private final String embeddingModel;
    private final List<KnowledgeDocument> documents = new ArrayList<>();
    private final RestTemplate restTemplate = new RestTemplate();

    private MilvusClient milvusClient;
    private boolean milvusReady;
    private boolean milvusCollectionPrepared;
    private boolean docKeyFieldMissing;
    private boolean milvusSyncSuppressed;

    public RagServiceImpl(
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            @Value("${rag.dataset-path:Util/standardList.jsonl}") String datasetPath,
            @Value("${rag.default-top-k:3}") int defaultTopK,
            @Value("${rag.collection-name:omnisource_rag}") String collectionName,
            @Value("${milvus.host:127.0.0.1}") String milvusHost,
            @Value("${milvus.port:19530}") int milvusPort,
            @Value("${milvus.database:default}") String milvusDatabase,
            @Value("${milvus.username:}") String milvusUsername,
            @Value("${milvus.password:}") String milvusPassword,
            @Value("${rag.milvus-enabled:false}") boolean milvusEnabled,
            @Value("${qianwen.api-key:}") String qianwenApiKey,
            @Value("${qianwen.base-url:https://dashscope.aliyuncs.com/compatible-mode}") String qianwenBaseUrl,
            @Value("${qianwen.embedding-model:text-embedding-v3}") String embeddingModel) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.datasetPath = resolveDatasetPath(datasetPath);
        this.defaultTopK = defaultTopK;
        this.collectionName = collectionName;
        this.milvusHost = StringUtils.hasText(milvusHost) ? milvusHost.trim() : "127.0.0.1";
        this.milvusPort = milvusPort > 0 ? milvusPort : 19530;
        this.milvusDatabase = StringUtils.hasText(milvusDatabase) ? milvusDatabase : "default";
        this.milvusUsername = milvusUsername;
        this.milvusPassword = milvusPassword;
        this.milvusEnabled = milvusEnabled;
        this.qianwenApiKey = qianwenApiKey;
        this.embeddingEndpoint = resolveEmbeddingEndpoint(qianwenBaseUrl);
        this.embeddingModel = embeddingModel;
        if (this.milvusEnabled) {
            initializeMilvusClient();
        }
    }

    @PostConstruct
    public void init() {
        try {
            documents.clear();
            loadLocalDocuments();
        } catch (Exception e) {
            milvusReady = false;
            log.warn("RAG init skipped, falling back to local retrieval only: {}", e.getMessage(), e);
        }
    }

    @Scheduled(
            fixedDelayString = "${rag.sync-fixed-delay-ms:3600000}",
            initialDelayString = "${rag.sync-initial-delay-ms:10000}"
    )
    public void scheduledSync() {
        try {
            refreshAndSync();
        } catch (Exception e) {
            milvusReady = false;
            log.warn("RAG scheduled sync failed, keeping local retrieval only: {}", e.getMessage(), e);
        }
    }

    @Override
    public synchronized void reload() {
        try {
            refreshAndSync();
        } catch (Exception e) {
            milvusReady = false;
            log.warn("RAG reload failed, keeping local retrieval only: {}", e.getMessage(), e);
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
                log.warn("Milvus retrieval failed, fallback to local search: {}", e.getMessage());
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
            return "No related knowledge found.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Related knowledge:\n");
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
                    builder.append("- level: ").append(level).append('\n');
                }
                if (category != null) {
                    builder.append("- category: ").append(category).append('\n');
                }
                if (region != null) {
                    builder.append("- region: ").append(region).append('\n');
                }
            }

            builder.append("- content: ").append(item.getContent()).append('\n');
        }

        return builder.toString();
    }

    @Override
    public synchronized boolean isReady() {
        return !documents.isEmpty();
    }

    private synchronized void refreshAndSync() {
        documents.clear();
        loadLocalDocuments();
        syncLocalDocuments();
    }

    private void syncLocalDocuments() {
        if (documents.isEmpty()) {
            milvusReady = false;
            return;
        }

        if (!milvusEnabled || milvusSyncSuppressed) {
            milvusReady = false;
            return;
        }

        if (milvusClient == null) {
            initializeMilvusClient();
        }
        if (milvusClient == null) {
            milvusReady = false;
            return;
        }

        if (shouldRebuildBeforeSync()) {
            log.info("Milvus schema version mismatch detected, rebuilding collection first: {}", collectionName);
            if (rebuildCollectionAndResync()) {
                markSchemaVersion();
            }
            return;
        }

        List<SyncCandidate> candidates = new ArrayList<>();
        Integer embeddingDimension = null;
        docKeyFieldMissing = false;

        for (KnowledgeDocument doc : documents) {
            String docKey = resolveDocKey(doc);
            if (!StringUtils.hasText(docKey)) {
                continue;
            }

            String fingerprint = fingerprint(doc);
            String cachedFingerprint = milvusCollectionPrepared ? readCachedFingerprint(docKey) : "";
            if (milvusCollectionPrepared && fingerprint.equals(cachedFingerprint)) {
                continue;
            }

            try {
                List<Float> embedding = embedText(buildEmbeddingText(doc));
                if (embedding.isEmpty()) {
                    continue;
                }
                if (embeddingDimension == null) {
                    embeddingDimension = embedding.size();
                }

                if (milvusCollectionPrepared) {
                    List<ExistingDocument> existingDocuments = queryExistingDocuments(docKey);
                    if (docKeyFieldMissing) {
                        break;
                    }
                    if (!existingDocuments.isEmpty()) {
                        boolean alreadySynced = existingDocuments.stream()
                                .anyMatch(existing -> fingerprint.equals(existing.fingerprint()));
                        if (alreadySynced) {
                            cacheFingerprint(docKey, fingerprint);
                            continue;
                        }
                        deleteExistingDocuments(existingDocuments);
                    }
                }

                candidates.add(new SyncCandidate(doc, docKey, fingerprint, embedding));
            } catch (Exception e) {
                log.warn("RAG document sync failed: {}", e.getMessage());
            }
        }

        if (docKeyFieldMissing) {
            log.warn("Detected legacy Milvus schema without doc_key. Rebuilding collection: {}", collectionName);
            rebuildCollectionAndResync();
            return;
        }

        if (candidates.isEmpty()) {
            milvusReady = safeLoadCollection();
            if (milvusReady) {
                markSchemaVersion();
            }
            return;
        }

        if (embeddingDimension == null || !ensureMilvusCollection(embeddingDimension)) {
            milvusReady = false;
            return;
        }

        insertCandidates(candidates);
        for (SyncCandidate candidate : candidates) {
            cacheFingerprint(candidate.docKey(), candidate.fingerprint());
        }

        try {
            milvusReady = safeFlushAndLoad();
        } catch (RuntimeException e) {
            milvusReady = false;
            log.warn("Milvus sync finish load failed: {}", e.getMessage());
        }
    }

    private boolean rebuildCollectionAndResync() {
        if (milvusClient == null) {
            milvusReady = false;
            return false;
        }

        try {
            milvusClient.dropCollection(DropCollectionParam.newBuilder()
                    .withDatabaseName(milvusDatabase)
                    .withCollectionName(collectionName)
                    .build());
        } catch (RuntimeException e) {
            log.warn("Drop collection skipped/failed (may not exist): {}", e.getMessage());
        }

        milvusCollectionPrepared = false;
        docKeyFieldMissing = false;

        List<SyncCandidate> allCandidates = new ArrayList<>();
        Integer dimension = null;
        for (KnowledgeDocument doc : documents) {
            List<Float> embedding = embedText(buildEmbeddingText(doc));
            if (embedding.isEmpty()) {
                continue;
            }
            if (dimension == null) {
                dimension = embedding.size();
            }
            allCandidates.add(new SyncCandidate(doc, resolveDocKey(doc), fingerprint(doc), embedding));
        }

        if (dimension == null || allCandidates.isEmpty()) {
            milvusReady = false;
            return false;
        }

        if (!ensureMilvusCollection(dimension)) {
            milvusReady = false;
            return false;
        }

        insertCandidates(allCandidates);
        for (SyncCandidate candidate : allCandidates) {
            cacheFingerprint(candidate.docKey(), candidate.fingerprint());
        }

        try {
            milvusReady = safeFlushAndLoad();
            if (milvusReady) {
                markSchemaVersion();
            }
            return milvusReady;
        } catch (RuntimeException e) {
            milvusReady = false;
            log.warn("Milvus load after rebuild failed: {}", e.getMessage());
            return false;
        }
    }

    private void initializeMilvusClient() {
        try {
            ConnectParam.Builder builder = ConnectParam.newBuilder()
                    .withHost(milvusHost)
                    .withPort(milvusPort)
                    .withDatabaseName(milvusDatabase);

            if (StringUtils.hasText(milvusUsername) && StringUtils.hasText(milvusPassword)) {
                String authorization = java.util.Base64.getEncoder()
                        .encodeToString((milvusUsername + ":" + milvusPassword).getBytes(StandardCharsets.UTF_8));
                builder.withAuthorization(authorization);
            }

            milvusClient = new MilvusServiceClient(builder.build());
            log.info("Milvus client ready: host={}, port={}, db={}", milvusHost, milvusPort, milvusDatabase);
        } catch (RuntimeException e) {
            milvusClient = null;
            log.warn("Milvus client init failed: {}", e.getMessage(), e);
        }
    }

    private boolean ensureMilvusCollection(int dimension) {
        if (milvusCollectionPrepared) {
            return true;
        }

        try {
            List<FieldType> fieldTypes = List.of(
                    FieldType.newBuilder()
                            .withName(FIELD_ID)
                            .withDataType(DataType.Int64)
                            .withPrimaryKey(true)
                            .withAutoID(true)
                            .build(),
                    FieldType.newBuilder()
                            .withName(FIELD_DOC_KEY)
                            .withDataType(DataType.VarChar)
                            .withMaxLength(DOC_KEY_MAX_LENGTH)
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
                            .withName(FIELD_METADATA)
                            .withDataType(DataType.VarChar)
                            .withMaxLength(METADATA_MAX_LENGTH)
                            .build(),
                    FieldType.newBuilder()
                            .withName(FIELD_EMBEDDING)
                            .withDataType(DataType.FloatVector)
                            .withDimension(dimension)
                            .build());

            milvusClient.createCollection(CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDatabaseName(milvusDatabase)
                    .withDescription("OmniSource RAG knowledge base")
                    .withShardsNum(1)
                    .withFieldTypes(fieldTypes)
                    .build());

            milvusClient.createIndex(CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDatabaseName(milvusDatabase)
                    .withFieldName(FIELD_EMBEDDING)
                    .withIndexType(IndexType.AUTOINDEX)
                    .withIndexName("embedding_index")
                    .withMetricType(MetricType.COSINE)
                    .build());

            milvusCollectionPrepared = true;
            return true;
        } catch (Exception e) {
            handleMilvusTransportFailure(e, "prepare collection");
            if (isCollectionAlreadyExists(e)) {
                milvusCollectionPrepared = true;
                return true;
            }
            log.warn("Milvus collection prepare failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private void insertCandidates(List<SyncCandidate> candidates) {
        if (candidates.isEmpty()) {
            return;
        }

        int start = 0;
        while (start < candidates.size()) {
            int end = Math.min(start + INSERT_BATCH_SIZE, candidates.size());
            insertCandidateBatchWithFallback(candidates.subList(start, end));
            start = end;
        }
    }

    private void insertCandidateBatchWithFallback(List<SyncCandidate> batch) {
        try {
            insertCandidateBatch(batch);
        } catch (Exception firstFailure) {
            if (batch.size() <= 1) {
                SyncCandidate candidate = batch.get(0);
                log.warn("Milvus insert skipped for docKey={}, reason={}", candidate.docKey(), firstFailure.getMessage());
                return;
            }

            int mid = batch.size() / 2;
            insertCandidateBatchWithFallback(batch.subList(0, mid));
            insertCandidateBatchWithFallback(batch.subList(mid, batch.size()));
        }
    }

    private void insertCandidateBatch(List<SyncCandidate> batch) {
        if (batch.isEmpty()) {
            return;
        }

        int dimension = batch.get(0).embedding().size();
        List<String> docKeys = new ArrayList<>(batch.size());
        List<String> titles = new ArrayList<>(batch.size());
        List<String> contents = new ArrayList<>(batch.size());
        List<String> sources = new ArrayList<>(batch.size());
        List<String> metadataJsonList = new ArrayList<>(batch.size());
        List<List<Float>> embeddings = new ArrayList<>(batch.size());

        for (SyncCandidate candidate : batch) {
            if (candidate.embedding().size() != dimension) {
                throw new IllegalStateException("Embedding dimension mismatch");
            }
            docKeys.add(truncate(safeText(candidate.docKey()), DOC_KEY_MAX_LENGTH));
            titles.add(truncate(safeText(candidate.doc().title()), TITLE_MAX_LENGTH));
            contents.add(truncate(safeText(candidate.doc().content()), CONTENT_MAX_LENGTH));
            sources.add(truncate(resolveSource(candidate.doc()), SOURCE_MAX_LENGTH));
            metadataJsonList.add(truncate(serializeMilvusMetadata(candidate.doc().metadata()), METADATA_MAX_LENGTH));
            embeddings.add(candidate.embedding());
        }

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(FIELD_DOC_KEY, docKeys));
        fields.add(new InsertParam.Field(FIELD_TITLE, titles));
        fields.add(new InsertParam.Field(FIELD_CONTENT, contents));
        fields.add(new InsertParam.Field(FIELD_SOURCE, sources));
        fields.add(new InsertParam.Field(FIELD_METADATA, metadataJsonList));
        fields.add(new InsertParam.Field(FIELD_EMBEDDING, embeddings));

        try {
            milvusClient.insert(InsertParam.newBuilder()
                    .withDatabaseName(milvusDatabase)
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build());
        } catch (Exception e) {
            log.warn("Milvus insert batch failed: {}", e.getMessage());
            throw e instanceof RuntimeException runtimeException ? runtimeException : new RuntimeException(e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void deleteExistingDocuments(List<ExistingDocument> existingDocuments) {
        List<Long> ids = existingDocuments.stream()
                .map(ExistingDocument::primaryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }

        String expr = FIELD_ID + " in [" + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
        milvusClient.delete(DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build());
    }

    private List<ExistingDocument> queryExistingDocuments(String docKey) {
        try {
            String expr = FIELD_DOC_KEY + " == " + quote(docKey);
            R<?> response = milvusClient.query(QueryParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr(expr)
                    .withOutFields(List.of(FIELD_ID, FIELD_DOC_KEY, FIELD_TITLE, FIELD_CONTENT, FIELD_SOURCE, FIELD_METADATA))
                    .build());

            if (response == null || response.getData() == null) {
                return List.of();
            }

            QueryResultsWrapper wrapper = new QueryResultsWrapper((io.milvus.grpc.QueryResults) response.getData());
            List<QueryResultsWrapper.RowRecord> rows = wrapper.getRowRecords();
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }

            List<ExistingDocument> existingDocuments = new ArrayList<>(rows.size());
            for (QueryResultsWrapper.RowRecord row : rows) {
                Long primaryId = toLong(row.get(FIELD_ID));
                String title = safeText(asString(row.get(FIELD_TITLE)));
                String content = safeText(asString(row.get(FIELD_CONTENT)));
                String source = safeText(asString(row.get(FIELD_SOURCE)));
                String metadata = safeText(asString(row.get(FIELD_METADATA)));
                String fingerprint = fingerprintFromFields(docKey, title, content, source, metadata);
                existingDocuments.add(new ExistingDocument(primaryId, fingerprint));
            }
            return existingDocuments;
        } catch (Exception e) {
            handleMilvusTransportFailure(e, "query existing documents");
            if (isDocKeyMissingError(e)) {
                docKeyFieldMissing = true;
                log.warn("Milvus schema mismatch detected: {}", e.getMessage());
                return List.of();
            }
            log.warn("Query synced docs failed: {}", e.getMessage());
            return List.of();
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

        R<?> response = milvusClient.search(SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withTopK(topK)
                .withVectors(List.of(queryEmbedding))
                .withVectorFieldName(FIELD_EMBEDDING)
                .withOutFields(List.of(FIELD_DOC_KEY, FIELD_TITLE, FIELD_CONTENT, FIELD_SOURCE, FIELD_METADATA))
                .build());

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
            String docKey = safeText(asString(fieldValues.get(FIELD_DOC_KEY)));
            String title = safeText(asString(fieldValues.get(FIELD_TITLE)));
            String content = safeText(asString(fieldValues.get(FIELD_CONTENT)));
            String source = safeText(asString(fieldValues.get(FIELD_SOURCE)));
            Map<String, Object> metadata = parseMetadata(asString(fieldValues.get(FIELD_METADATA)));
            if (StringUtils.hasText(source)) {
                metadata.putIfAbsent("source", source);
            }

            results.add(RagRetrievalResponse.builder()
                    .id(StringUtils.hasText(docKey) ? docKey : String.valueOf(score.getLongID()))
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

    private void loadLocalDocuments() {
        if (!Files.exists(datasetPath)) {
            log.warn("RAG dataset not found: {}", datasetPath.toAbsolutePath());
            log.warn("Working directory: {}", Paths.get(".").toAbsolutePath().normalize());
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

            log.info("RAG dataset loaded: {} docs, file={}", documents.size(), datasetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load RAG dataset: {}", datasetPath.toAbsolutePath(), e);
        }
    }

    private void cacheFingerprint(String docKey, String fingerprint) {
        String redisKey = redisKey();
        redisTemplate.opsForHash().put(redisKey, docKey, fingerprint);
        redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);
    }

    private String readCachedFingerprint(String docKey) {
        Object value = redisTemplate.opsForHash().get(redisKey(), docKey);
        return value == null ? "" : String.valueOf(value);
    }

    private String redisKey() {
        return REDIS_HASH_PREFIX + collectionName;
    }

    private String schemaVersionKey() {
        return REDIS_SCHEMA_VERSION_KEY + collectionName;
    }

    private boolean shouldRebuildBeforeSync() {
        Object current = redisTemplate.opsForValue().get(schemaVersionKey());
        if (current == null) {
            return false;
        }
        return !CURRENT_SCHEMA_VERSION.equals(String.valueOf(current));
    }

    private void markSchemaVersion() {
        redisTemplate.opsForValue().set(schemaVersionKey(), CURRENT_SCHEMA_VERSION, 180, TimeUnit.DAYS);
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
                log.warn("Unexpected embedding response: {}", responseBody);
                return List.of();
            }

            List<Float> embedding = new ArrayList<>(embeddingNode.size());
            for (JsonNode value : embeddingNode) {
                embedding.add((float) value.asDouble());
            }
            return embedding;
        } catch (IOException | RestClientException e) {
            log.warn("Embedding call failed: {}", e.getMessage());
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
        return value.toLowerCase().replaceAll("[\\p{P}\\p{S}\\s]+", "").trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String fingerprint(KnowledgeDocument doc) {
        return fingerprintFromFields(
                resolveDocKey(doc),
                safeText(doc.title()),
                safeText(doc.content()),
                resolveSource(doc),
                serializeMetadata(doc.metadata())
        );
    }

    private String fingerprintFromFields(String docKey, String title, String content, String source, String metadata) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(safeText(docKey).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(safeText(title).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(safeText(content).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(safeText(source).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(safeText(metadata).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        Map<String, Object> ordered = new TreeMap<>(metadata);
        try {
            return objectMapper.writeValueAsString(ordered);
        } catch (Exception e) {
            return ordered.toString();
        }
    }

    private String serializeMilvusMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        putMetadataValue(summary, metadata, "source");
        putMetadataValue(summary, metadata, "path");
        putMetadataValue(summary, metadata, "sheet");
        putMetadataValue(summary, metadata, "row");
        putMetadataValue(summary, metadata, "level");
        putMetadataValue(summary, metadata, "category");
        putMetadataValue(summary, metadata, "region");
        putMetadataValue(summary, metadata, "type");
        putMetadataValue(summary, metadata, "tag");

        if (summary.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return summary.toString();
        }
    }

    private void putMetadataValue(Map<String, Object> target, Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return;
        }
        String text = truncate(safeText(asString(value)), 512);
        if (StringUtils.hasText(text)) {
            target.put(key, text);
        }
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String quote(String value) {
        String escaped = safeText(value).replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
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
                log.info("RAG dataset path resolved: {}", normalized.toAbsolutePath());
                return normalized;
            }
        }

        Path fallback = Paths.get(configuredPath).normalize();
        log.warn("No usable RAG dataset path found, fallback to: {}", fallback.toAbsolutePath());
        return fallback;
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

    private String resolveDocKey(KnowledgeDocument doc) {
        String docKey = safeText(doc.id());
        if (StringUtils.hasText(docKey)) {
            return docKey;
        }
        docKey = safeText(doc.title());
        if (StringUtils.hasText(docKey)) {
            return docKey;
        }
        docKey = safeText(resolveSource(doc));
        if (StringUtils.hasText(docKey)) {
            return docKey;
        }
        return safeText(fingerprintFromFields("", safeText(doc.title()), safeText(doc.content()), resolveSource(doc), serializeMetadata(doc.metadata())));
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

    private boolean safeLoadCollection() {
        try {
            milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withDatabaseName(milvusDatabase)
                    .withCollectionName(collectionName)
                    .build());
            return true;
        } catch (Exception e) {
            handleMilvusTransportFailure(e, "load collection");
            log.warn("Milvus load failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean safeFlushAndLoad() {
        try {
            milvusClient.flush(FlushParam.newBuilder()
                    .withDatabaseName(milvusDatabase)
                    .withCollectionNames(List.of(collectionName))
                    .withSyncFlush(Boolean.TRUE)
                    .build());
            return safeLoadCollection();
        } catch (Exception e) {
            handleMilvusTransportFailure(e, "flush collection");
            log.warn("Milvus flush failed: {}", e.getMessage());
            return false;
        }
    }

    private void handleMilvusTransportFailure(Exception e, String action) {
        if (milvusSyncSuppressed || !isMilvusTransportFailure(e)) {
            return;
        }

        milvusSyncSuppressed = true;
        milvusReady = false;
        milvusCollectionPrepared = false;
        milvusClient = null;
        log.warn("Milvus {} failed due to transport error, suppressing further Milvus sync in this run: {}", action, e.getMessage());
    }

    private boolean isMilvusTransportFailure(Exception e) {
        String message = e == null ? "" : String.valueOf(e.getMessage());
        String text = message.toUpperCase();
        return text.contains("UNAVAILABLE")
                || text.contains("NETWORK CLOSED")
                || text.contains("CONNECTION REFUSED")
                || text.contains("TRANSPORT CLOSED");
    }

    private boolean isCollectionAlreadyExists(Exception e) {
        String message = e.getMessage();
        return StringUtils.hasText(message)
                && (message.contains("already exists") || message.contains("exist") || message.contains("Already exists"));
    }

    private boolean isDocKeyMissingError(Exception e) {
        String message = e.getMessage();
        return StringUtils.hasText(message)
                && message.contains("field doc_key not exist");
    }

    private record KnowledgeDocument(String id, String title, String content, Map<String, Object> metadata) {
    }

    private record SyncCandidate(KnowledgeDocument doc, String docKey, String fingerprint, List<Float> embedding) {
    }

    private record ExistingDocument(Long primaryId, String fingerprint) {
    }
}
