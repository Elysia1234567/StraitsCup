package com.omnisource.draft.rag;

import com.omnisource.draft.model.KnowledgeChunk;
import java.util.List;
import java.util.Map;

public interface VectorStore {

    void upsert(List<KnowledgeChunk> chunks);

    List<KnowledgeChunk> similaritySearch(String query, int topK, Map<String, Object> filters);
}
