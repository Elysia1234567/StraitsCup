package com.omnisource.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpToolCallResponse {
    private String toolName;
    private boolean success;
    private Object result;
    private String error;
}
