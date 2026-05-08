package com.omnisource.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class McpToolDescriptor {
    private String name;
    private String title;
    private String description;
    private Map<String, Object> inputSchema;
}
