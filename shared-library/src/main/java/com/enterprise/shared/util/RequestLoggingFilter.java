package com.enterprise.shared.util;

import com.enterprise.shared.constant.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Generates/propagates a request id, injects it plus user + API path into
 * the MDC (so every log line for the request is structured and correlated),
 * and logs a single structured summary line per request including
 * execution time and response code. This is the backbone that later lets
 * an OpenTelemetry Collector / log-based metrics pipeline correlate logs
 * with traces via the request id.
 */
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.enterprise.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader(AppConstants.HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String api = request.getMethod() + " " + request.getRequestURI();
        MDC.put(AppConstants.MDC_REQUEST_ID, requestId);
        MDC.put(AppConstants.MDC_API, api);
        response.setHeader(AppConstants.HEADER_REQUEST_ID, requestId);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String user = MDC.get(AppConstants.MDC_USER);
            log.info("requestId={} user={} api={} status={} durationMs={}",
                    requestId, user != null ? user : "anonymous", api, response.getStatus(), durationMs);
            MDC.remove(AppConstants.MDC_REQUEST_ID);
            MDC.remove(AppConstants.MDC_API);
        }
    }
}
