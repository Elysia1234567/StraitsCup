package com.omnisource.controller;

import com.omnisource.dto.response.RagRetrievalResponse;
import com.omnisource.service.RagService;
import com.omnisource.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 最小闭环操作接口。
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 一键重建知识库：建 Milvus collection + 导入 JSONL + 建索引。
     */
    @PostMapping("/reload")
    public Result<Map<String, Object>> reload() {
        ragService.reload();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ready", ragService.isReady());
        data.put("message", "RAG 知识库已重建");
        return Result.success(data);
    }

    /**
     * 向量检索调试接口。
     */
    @GetMapping("/retrieve")
    public Result<List<RagRetrievalResponse>> retrieve(
            @RequestParam("question") String question,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {
        return Result.success(ragService.retrieve(question, topK));
    }

    /**
     * Prompt 预览接口（已拼入检索结果）。
     */
    @GetMapping("/prompt")
    public Result<String> prompt(
            @RequestParam("question") String question,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {
        return Result.success(ragService.buildContext(question, topK));
    }
}
