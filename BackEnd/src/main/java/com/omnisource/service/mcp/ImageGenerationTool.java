package com.omnisource.service.mcp;

import com.omnisource.dto.response.McpToolDescriptor;
import com.omnisource.service.ImageGenerationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ImageGenerationTool extends AbstractMcpTool {

    private final ImageGenerationService imageGenerationService;

    public ImageGenerationTool(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    @Override
    protected String name() {
        return "image_generate";
    }

    @Override
    public McpToolDescriptor descriptor() {
        return descriptor(
                "Image generation",
                "Generate an image with the configured Qwen image model and upload it to OSS.",
                ToolSchemas.objectSchema(
                        Map.of(
                                "prompt", ToolSchemas.property("string", "Image prompt."),
                                "style", ToolSchemas.property("string", "Optional style label."),
                                "userId", ToolSchemas.property("integer", "Optional user id."),
                                "roomId", ToolSchemas.property("integer", "Optional chat room id."),
                                "referenceImageUrl", ToolSchemas.property("string", "Optional reference image URL.")
                        ),
                        List.of("prompt")
                )
        );
    }

    @Override
    protected Object execute(Map<String, Object> arguments) {
        String prompt = stringArg(arguments, "prompt", true);
        String style = stringArg(arguments, "style", false);
        String referenceImageUrl = stringArg(arguments, "referenceImageUrl", false);
        Long userId = longArg(arguments, "userId");
        Long roomId = longArg(arguments, "roomId");
        String url = imageGenerationService.generateImageAndReturnUrl(
                userId == null ? 1L : userId,
                roomId,
                prompt,
                style,
                referenceImageUrl
        );
        return Map.of("imageUrl", url);
    }
}
