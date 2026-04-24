package com.omnisource.draft.rag;

import com.omnisource.draft.model.KnowledgeChunk;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MockRetrievalService implements RetrievalService {

    @Override
    public List<KnowledgeChunk> search(String query, int topK, Map<String, Object> filters) {
        List<KnowledgeChunk> chunks = new ArrayList<>();

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setChunkId("chunk_mock_001");
        chunk.setHeritageId(String.valueOf(filters.getOrDefault("heritageId", "unknown")));
        chunk.setSource("mock-source");
        chunk.setScore(0.99);
        chunk.setContent("这是用于联调的模拟知识片段，可替换为 Milvus 或 Redis Vector 检索结果。");
        chunks.add(chunk);

        return chunks;
    }
}
