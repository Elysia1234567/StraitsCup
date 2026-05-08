package com.omnisource.service.mcp;

import com.omnisource.dto.response.McpToolDescriptor;
import com.omnisource.service.TavilySearchService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TavilySearchTool extends AbstractMcpTool {

    private final TavilySearchService tavilySearchService;

    public TavilySearchTool(TavilySearchService tavilySearchService) {
        this.tavilySearchService = tavilySearchService;
    }

    @Override
    protected String name() {
        return "tavily_search";
    }

    @Override
    public McpToolDescriptor descriptor() {
        return descriptor(
                "Tavily search",
                "Search the web with Tavily and return a concise formatted reference block.",
                ToolSchemas.objectSchema(
                        Map.of("query", ToolSchemas.property("string", "Search query.")),
                        List.of("query")
                )
        );
    }

    @Override
    protected Object execute(Map<String, Object> arguments) {
        String query = stringArg(arguments, "query", true);
        return Map.of("text", tavilySearchService.searchAndFormat(query));
    }
}
