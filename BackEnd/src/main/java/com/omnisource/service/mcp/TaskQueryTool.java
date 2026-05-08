package com.omnisource.service.mcp;

import com.omnisource.dto.response.McpToolDescriptor;
import com.omnisource.service.ImageGenerationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TaskQueryTool extends AbstractMcpTool {

    private final ImageGenerationService imageGenerationService;

    public TaskQueryTool(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    @Override
    protected String name() {
        return "task_query";
    }

    @Override
    public McpToolDescriptor descriptor() {
        return descriptor(
                "Task query",
                "Query image generation task status by task id.",
                ToolSchemas.objectSchema(
                        Map.of("taskId", ToolSchemas.property("string", "Image generation task id.")),
                        List.of("taskId")
                )
        );
    }

    @Override
    protected Object execute(Map<String, Object> arguments) {
        String taskId = stringArg(arguments, "taskId", true);
        return imageGenerationService.getTask(taskId);
    }
}
