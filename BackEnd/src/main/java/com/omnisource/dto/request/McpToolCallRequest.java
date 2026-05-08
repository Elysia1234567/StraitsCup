package com.omnisource.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class McpToolCallRequest {

    @NotNull(message = "arguments is required")
    private Map<String, Object> arguments = new LinkedHashMap<>();
}
