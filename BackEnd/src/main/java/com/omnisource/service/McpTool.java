package com.omnisource.service;

import com.omnisource.dto.response.McpToolCallResponse;
import com.omnisource.dto.response.McpToolDescriptor;

import java.util.Map;

public interface McpTool {
    McpToolDescriptor descriptor();

    McpToolCallResponse call(Map<String, Object> arguments);
}
