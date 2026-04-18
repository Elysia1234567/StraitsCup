package com.omnisource.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 日志追踪过滤器
 * 为每个请求生成唯一的 traceId，便于日志追踪和问题排查
 *
 * @author OmniSource
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@WebFilter(urlPatterns = "/*")
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 生成唯一的 traceId
            String traceId = UUID.randomUUID().toString().replace("-", "");
            MDC.put(TRACE_ID, traceId);

            // 继续执行请求
            chain.doFilter(request, response);
        } finally {
            // 请求处理完成后清除 traceId
            MDC.remove(TRACE_ID);
        }
    }
}
