package com.ims.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Wrap request and response to read body multiple times
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();

        // Log request
        logRequest(wrappedRequest);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log response
            logResponse(wrappedRequest, wrappedResponse, duration);

            // Important: copy the cached response content to the actual response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String queryString = request.getQueryString();

        log.info("╔════════════════════════════════════════════════════════════");
        log.info("║ 📥 INCOMING REQUEST");
        log.info("║ Method: {} {}", method, uri);

        if (queryString != null) {
            log.info("║ Query: {}", queryString);
        }

        log.info("║ Headers:");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            String headerValue = request.getHeader(headerName);
            // Don't log sensitive headers
            if (headerName.equalsIgnoreCase("Authorization")) {
                headerValue = "Bearer ***";
            }
            log.info("║   {}: {}", headerName, headerValue);
        });

        // Log request body for POST/PUT/PATCH
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                // Don't log passwords
                if (body.contains("password")) {
                    body = body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
                }
                log.info("║ Body: {}", body);
            }
        }

        log.info("╚════════════════════════════════════════════════════════════");
    }

    private void logResponse(ContentCachingRequestWrapper request,
                             ContentCachingResponseWrapper response,
                             long duration) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();

        log.info("╔════════════════════════════════════════════════════════════");
        log.info("║ 📤 OUTGOING RESPONSE");
        log.info("║ Method: {} {}", method, uri);
        log.info("║ Status: {}", status);
        log.info("║ Duration: {} ms", duration);

        log.info("║ Headers:");
        response.getHeaderNames().forEach(headerName -> {
            log.info("║   {}: {}", headerName, response.getHeader(headerName));
        });

        byte[] content = response.getContentAsByteArray();
        if (content.length > 0 && content.length < 10000) { // Don't log huge responses
            String body = new String(content, StandardCharsets.UTF_8);
            log.info("║ Body: {}", body);
        }

        log.info("╚════════════════════════════════════════════════════════════");
    }
}