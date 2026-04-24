package com.omnisource.draft.rag;

import com.omnisource.draft.model.KnowledgeChunk;
import java.util.List;
import java.util.Map;

public interface RetrievalService {

    List<KnowledgeChunk> search(String query, int topK, Map<String, Object> filters);
}
