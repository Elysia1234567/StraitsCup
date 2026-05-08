package com.omnisource.service.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ToolSchemas {

    private ToolSchemas() {
    }

    static Map<String, Object> objectSchema(Map<String, ?> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    static Map<String, Object> property(String type, String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", type);
        property.put("description", description);
        return property;
    }
}
