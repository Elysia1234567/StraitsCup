package com.omnisource.service.mcp;

import com.omnisource.dto.response.McpToolCallResponse;
import com.omnisource.dto.response.McpToolDescriptor;
import com.omnisource.service.McpTool;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
abstract class AbstractMcpTool implements McpTool {

    protected abstract String name();

    protected abstract Object execute(Map<String, Object> arguments);

    @Override
    public final McpToolCallResponse call(Map<String, Object> arguments) {
        try {
            return McpToolCallResponse.builder()
                    .toolName(name())
                    .success(true)
                    .result(execute(arguments))
                    .build();
        } catch (Exception e) {
            log.warn("MCP tool call failed: tool={}, error={}", name(), e.getMessage());
            return McpToolCallResponse.builder()
                    .toolName(name())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    protected String stringArg(Map<String, Object> arguments, String name, boolean required) {
        Object value = arguments.get(name);
        if (value == null || value.toString().isBlank()) {
            if (required) {
                throw new IllegalArgumentException(name + " is required");
            }
            return null;
        }
        return value.toString();
    }

    protected int intArg(Map<String, Object> arguments, String name, int defaultValue, int min, int max) {
        Object value = arguments.get(name);
        int parsed = defaultValue;
        if (value instanceof Number number) {
            parsed = number.intValue();
        } else if (value != null && !value.toString().isBlank()) {
            parsed = Integer.parseInt(value.toString());
        }
        return Math.max(min, Math.min(max, parsed));
    }

    protected Long longArg(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    protected McpToolDescriptor descriptor(String title, String description, Map<String, Object> schema) {
        return McpToolDescriptor.builder()
                .name(name())
                .title(title)
                .description(description)
                .inputSchema(schema)
                .build();
    }
}
