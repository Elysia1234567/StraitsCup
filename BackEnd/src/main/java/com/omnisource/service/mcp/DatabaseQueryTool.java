package com.omnisource.service.mcp;

import com.omnisource.dto.response.McpToolDescriptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DatabaseQueryTool extends AbstractMcpTool {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseQueryTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected String name() {
        return "database_query";
    }

    @Override
    public McpToolDescriptor descriptor() {
        return descriptor(
                "Database query",
                "Run a read-only SQL query against the OmniSource database.",
                ToolSchemas.objectSchema(
                        Map.of(
                                "sql", ToolSchemas.property("string", "Read-only SELECT/SHOW/DESCRIBE/EXPLAIN SQL."),
                                "limit", ToolSchemas.property("integer", "Maximum rows returned, 1-100.")
                        ),
                        List.of("sql")
                )
        );
    }

    @Override
    protected Object execute(Map<String, Object> arguments) {
        String sql = stringArg(arguments, "sql", true).trim();
        validateReadOnly(sql);
        int limit = intArg(arguments, "limit", 20, 1, 100);
        String limitedSql = appendLimitIfNeeded(sql, limit);
        return jdbcTemplate.queryForList(limitedSql);
    }

    private void validateReadOnly(String sql) {
        String normalized = sql.stripLeading().toLowerCase(Locale.ROOT);
        if (!(normalized.startsWith("select ")
                || normalized.startsWith("show ")
                || normalized.startsWith("describe ")
                || normalized.startsWith("desc ")
                || normalized.startsWith("explain "))) {
            throw new IllegalArgumentException("Only read-only SQL is allowed");
        }
        if (normalized.contains(";")) {
            throw new IllegalArgumentException("Only one SQL statement is allowed");
        }
        String[] forbidden = {"insert", "update", "delete", "drop", "alter", "truncate", "create"};
        for (String keyword : forbidden) {
            if (normalized.matches("(?s).*\\b" + keyword + "\\b.*")) {
                throw new IllegalArgumentException("Only read-only SQL is allowed");
            }
        }
    }

    private String appendLimitIfNeeded(String sql, int limit) {
        String normalized = sql.toLowerCase(Locale.ROOT);
        if (normalized.matches("(?s).*\\blimit\\s+\\d+\\s*$")) {
            return sql;
        }
        return sql + " LIMIT " + limit;
    }
}
