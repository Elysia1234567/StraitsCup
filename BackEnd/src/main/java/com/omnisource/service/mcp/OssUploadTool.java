package com.omnisource.service.mcp;

import com.omnisource.dto.response.McpToolDescriptor;
import com.omnisource.service.OssUploadService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class OssUploadTool extends AbstractMcpTool {

    private final OssUploadService ossUploadService;

    public OssUploadTool(OssUploadService ossUploadService) {
        this.ossUploadService = ossUploadService;
    }

    @Override
    protected String name() {
        return "oss_upload";
    }

    @Override
    public McpToolDescriptor descriptor() {
        return descriptor(
                "OSS upload",
                "Upload base64 file content to Aliyun OSS and return a public URL.",
                ToolSchemas.objectSchema(
                        Map.of(
                                "filename", ToolSchemas.property("string", "Original filename including extension."),
                                "contentBase64", ToolSchemas.property("string", "Base64 encoded file bytes. Data URL prefixes are accepted."),
                                "folder", ToolSchemas.property("string", "Optional OSS target folder.")
                        ),
                        List.of("filename", "contentBase64")
                )
        );
    }

    @Override
    protected Object execute(Map<String, Object> arguments) {
        String filename = stringArg(arguments, "filename", true);
        String contentBase64 = stringArg(arguments, "contentBase64", true);
        String folder = stringArg(arguments, "folder", false);
        String normalized = contentBase64.contains(",")
                ? contentBase64.substring(contentBase64.indexOf(',') + 1)
                : contentBase64;
        byte[] bytes = Base64.getDecoder().decode(normalized);
        String url = StringUtils.hasText(folder)
                ? ossUploadService.uploadImage(new ByteArrayInputStream(bytes), filename, folder)
                : ossUploadService.uploadImage(new ByteArrayInputStream(bytes), filename, null);
        return Map.of("url", url, "filename", filename, "size", bytes.length);
    }
}
