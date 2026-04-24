package com.omnisource.draft.llm;

import java.util.List;

public class MockLlmClient implements LlmClient {

    @Override
    public String chat(String systemPrompt, String userPrompt, List<String> references) {
        return "【Mock LLM】问题：" + userPrompt + "；参考资料数量：" + references.size();
    }
}
