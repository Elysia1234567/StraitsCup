package com.omnisource.draft.llm;

import java.util.List;

public interface LlmClient {

    String chat(String systemPrompt, String userPrompt, List<String> references);
}
