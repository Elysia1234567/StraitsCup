package com.omnisource.controller;

import com.omnisource.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统技术画像接口。
 *
 * <p>用于答辩、运维看板或前端展示系统能力边界，集中说明数据库、
 * AI 调度、RAG、WebSocket、AIGC 和前端 3D 渲染等核心模块。</p>
 */
@RestController
@RequestMapping("/api/system-profile")
public class SystemProfileController {

    @GetMapping
    public Result<Map<String, Object>> getSystemProfile() {
        return Result.success(Map.of(
                "name", "OmniSource",
                "displayName", "同源",
                "positioning", "基于多智能体与 AIGC 的非遗文化数字生命共创平台",
                "database", databaseProfile(),
                "aiOrchestration", aiOrchestrationProfile(),
                "frontendRendering", frontendRenderingProfile(),
                "dataFlow", dataFlowProfile(),
                "innovationKeywords", innovationKeywords()
        ));
    }

    private Map<String, Object> databaseProfile() {
        return Map.of(
                "engine", "MySQL + MyBatis",
                "tables", List.of(
                        "agent",
                        "cultural_theme",
                        "chat_room",
                        "chat_room_member",
                        "chat_message",
                        "image_generation_task"
                ),
                "vectorStore", "Milvus collection: omnisource_rag",
                "cache", "Redis: agent cooldown, generation task status, realtime state",
                "objectStorage", "Aliyun OSS: source images, generated images, chat room assets"
        );
    }

    private Map<String, Object> aiOrchestrationProfile() {
        return Map.of(
                "chatModel", "Spring AI OpenAI-compatible ChatClient",
                "embeddingModel", "text-embedding-v3 compatible embedding service",
                "imageModel", "qwen-image-2.0-pro compatible multimodal generation",
                "scheduler", "AgentOrchestrator relevance scoring + Redis cooldown",
                "promptEngine", "AgentPromptBuilder persona + RAG + web search + constraints",
                "streaming", "WebSocket AGENT_START / AGENT_CHUNK / AGENT_END event stream"
        );
    }

    private Map<String, Object> frontendRenderingProfile() {
        return Map.of(
                "framework", "Vue 3 + Vite + Tailwind CSS",
                "mapAndGallery", "ECharts province map + OGL circular gallery",
                "threeD", "Three.js / OGL WebGL scene, infinite grid, post-processing and model viewer",
                "interaction", "WebSocket streaming chat, fullscreen 3D gallery, agent image generation panel"
        );
    }

    private List<String> dataFlowProfile() {
        return List.of(
                "User input enters REST or WebSocket endpoint",
                "AgentOrchestrator selects relevant agents by knowledge scope and cooldown",
                "RagService retrieves heritage knowledge from Milvus or local fallback",
                "AgentPromptBuilder assembles persona, references and output rules",
                "Spring AI streams answer chunks back to WebSocket room",
                "ImageGenerationService generates image, uploads to OSS and persists chat message",
                "Vue frontend renders chat stream, images, map, gallery and 3D views"
        );
    }

    private List<String> innovationKeywords() {
        return List.of(
                "非遗器灵化",
                "多智能体群聊",
                "RAG 事实增强",
                "海峡文化叙事",
                "AIGC 视觉共创",
                "WebSocket 流式体验",
                "WebGL 沉浸展示",
                "可观测工程画像"
        );
    }
}
