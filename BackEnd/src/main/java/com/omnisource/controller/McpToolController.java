package com.omnisource.controller;

import com.omnisource.dto.request.McpToolCallRequest;
import com.omnisource.dto.response.McpToolCallResponse;
import com.omnisource.dto.response.McpToolDescriptor;
import com.omnisource.service.McpToolRegistry;
import com.omnisource.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpToolController {

    private final McpToolRegistry toolRegistry;

    @GetMapping("/tools")
    public Result<List<McpToolDescriptor>> listTools() {
        return Result.success(toolRegistry.listTools());
    }

    @PostMapping("/tools/{name}/call")
    public Result<McpToolCallResponse> callTool(
            @PathVariable String name,
            @Valid @RequestBody McpToolCallRequest request) {
        McpToolCallResponse response = toolRegistry.callTool(name, request.getArguments());
        if (!response.isSuccess()) {
            return Result.badRequest(response.getError());
        }
        return Result.success(response);
    }
}
