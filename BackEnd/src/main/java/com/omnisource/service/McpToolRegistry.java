package com.omnisource.service;

import com.omnisource.dto.response.McpToolCallResponse;
import com.omnisource.dto.response.McpToolDescriptor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class McpToolRegistry {

    private final Map<String, McpTool> tools;

    public McpToolRegistry(List<McpTool> toolList) {
        this.tools = new LinkedHashMap<>();
        for (McpTool tool : toolList) {
            this.tools.put(tool.descriptor().getName(), tool);
        }
    }

    public List<McpToolDescriptor> listTools() {
        return tools.values().stream()
                .map(McpTool::descriptor)
                .toList();
    }

    public McpToolCallResponse callTool(String name, Map<String, Object> arguments) {
        McpTool tool = tools.get(name);
        if (tool == null) {
            return McpToolCallResponse.builder()
                    .toolName(name)
                    .success(false)
                    .error("Unknown tool: " + name)
                    .build();
        }
        return tool.call(arguments == null ? Map.of() : arguments);
    }
}
