package id.payu.logging.filter;

import id.payu.logging.config.PayuLoggingProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Servlet filter that logs HTTP request and response details.
 * Controlled by {@code payu.logging.request-logging} properties.
 *
 * <p>When enabled, logs method, URI, status, duration, and optionally
 * request/response payloads (truncated to {@code maxPayloadLength}).</p>
 */
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "x-api-key", "x-api-secret"
    );

    private final PayuLoggingProperties.RequestLoggingProperties config;

    public RequestLoggingFilter(PayuLoggingProperties properties) {
        this.config = properties.getRequestLogging();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip actuator/health endpoints to reduce noise
        String path = httpRequest.getRequestURI();
        if (path.startsWith("/actuator") || path.equals("/health") || path.equals("/ready")) {
            chain.doFilter(request, response);
            return;
        }

        if (!config.isIncludePayload()) {
            // Fast path: no payload logging — no wrapping needed
            long start = System.currentTimeMillis();
            try {
                chain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - start;
                log.info("HTTP {} {} -> {} ({}ms)",
                        httpRequest.getMethod(), path,
                        httpResponse.getStatus(), duration);
            }
            return;
        }

        // Payload logging path: wrap request/response to cache body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(
                httpRequest, config.getMaxPayloadLength());
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String requestBody = truncate(wrappedRequest.getContentAsByteArray());
            String responseBody = truncate(wrappedResponse.getContentAsByteArray());

            log.info("HTTP {} {} -> {} ({}ms) | request={} | response={}",
                    httpRequest.getMethod(), path,
                    wrappedResponse.getStatus(), duration,
                    requestBody.isEmpty() ? "-" : requestBody,
                    responseBody.isEmpty() ? "-" : responseBody);

            // IMPORTANT: copy body back to the actual response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String truncate(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        int length = Math.min(content.length, config.getMaxPayloadLength());
        String body = new String(content, 0, length, StandardCharsets.UTF_8);
        if (content.length > config.getMaxPayloadLength()) {
            body += "...[truncated]";
        }
        return body;
    }
}
