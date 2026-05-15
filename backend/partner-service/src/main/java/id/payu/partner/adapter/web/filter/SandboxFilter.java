package id.payu.partner.adapter.web.filter;

import id.payu.partner.adapter.persistence.repository.ApiKeyRepository;
import id.payu.partner.adapter.persistence.entity.ApiKeyEntity;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that intercepts all partner API requests and handles sandbox mode routing.
 * <p>
 * When a request uses a sandbox API key:
 * - Adds X-Sandbox-Mode header for downstream services
 * - Routes to simulator services instead of production endpoints
 * - Logs sandbox activity for debugging
 * <p>
 * This filter runs early in the chain (Order 1) to ensure sandbox detection
 * happens before authentication and rate limiting.
 */
@Component
@Order(1)
public class SandboxFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SandboxFilter.class);

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String SANDBOX_MODE_HEADER = "X-Sandbox-Mode";
    private static final String SANDBOX_TARGET_HEADER = "X-Sandbox-Target";

    private final ApiKeyRepository apiKeyRepository;

    public SandboxFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String apiKey = extractApiKey(httpRequest);
        boolean isSandbox = false;

        if (apiKey != null && !apiKey.isEmpty()) {
            isSandbox = checkSandboxMode(apiKey);
            if (isSandbox) {
                log.debug("Sandbox mode detected for request: {} {}",
                        httpRequest.getMethod(), httpRequest.getRequestURI());
            }
        }

        // Wrap the request to add sandbox headers
        SandboxHttpServletRequestWrapper wrappedRequest =
                new SandboxHttpServletRequestWrapper(httpRequest, isSandbox);

        // Add sandbox headers to response for client awareness
        if (isSandbox) {
            httpResponse.addHeader(SANDBOX_MODE_HEADER, "true");
        }

        chain.doFilter(wrappedRequest, httpResponse);
    }

    /**
     * Extract API key from request headers.
     * Supports X-API-Key header.
     */
    private String extractApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isEmpty()) {
            // Also check Authorization header for Bearer token format
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                apiKey = authHeader.substring(7);
            }
        }
        return apiKey;
    }

    /**
     * Check if the API key is in sandbox mode.
     * Looks up the key hash in the database and checks the sandbox flag.
     */
    private boolean checkSandboxMode(String apiKey) {
        try {
            // Hash the API key to look it up (same hashing as storage)
            String keyHash = hashApiKey(apiKey);

            Optional<ApiKeyEntity> apiKeyEntity = apiKeyRepository.findByKeyHash(keyHash);
            if (apiKeyEntity.isPresent()) {
                ApiKeyEntity key = apiKeyEntity.get();
                boolean isSandbox = key.isSandbox() && key.isUsable();

                if (isSandbox) {
                    // Record usage for analytics
                    key.recordUsage();
                    apiKeyRepository.save(key);
                }

                return isSandbox;
            }
        } catch (Exception e) {
            log.warn("Error checking sandbox mode for API key: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Hash API key using SHA-256 (same as ApiKeyService).
     */
    private String hashApiKey(String apiKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
