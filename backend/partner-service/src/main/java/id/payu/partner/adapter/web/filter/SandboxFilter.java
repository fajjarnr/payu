package id.payu.partner.adapter.web.filter;

import id.payu.partner.application.service.ApiKeyService;
import id.payu.partner.adapter.persistence.entity.ApiKeyEntity;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that intercepts all partner API requests and handles sandbox mode routing.
 * <p>
 * When a request uses a sandbox API key:
 * - Adds X-Sandbox-Mode header for downstream services
 * - Routes to simulator services instead of production endpoints
 * - Logs sandbox activity for debugging
 * <p>
 * PARTNER-005: API key validation is delegated to {@link ApiKeyService#validateKey}
 * (the single production caller). A request presenting an {@code X-API-Key} header
 * is rejected with 401 when the key is unknown, revoked, or expired — fail closed.
 * The SNAP-BI bearer token is left untouched (it authenticates via client-key HMAC).
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

    private final ApiKeyService apiKeyService;

    public SandboxFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String apiKey = httpRequest.getHeader(API_KEY_HEADER);
        boolean isSandbox = false;

        // PARTNER-005: fail closed when an API key is presented but invalid.
        if (apiKey != null && !apiKey.isBlank()) {
            ApiKeyEntity key = apiKeyService.validateKey(apiKey);
            if (key == null) {
                log.warn("Rejecting request with invalid API key: {} {}",
                        httpRequest.getMethod(), httpRequest.getRequestURI());
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResponse.getWriter().write("{\"error_code\":\"AUTH_004\","
                        + "\"title\":\"Unauthorized\","
                        + "\"detail\":\"Invalid or revoked API key\"}");
                return;
            }
            isSandbox = key.isSandbox();
            if (isSandbox) {
                log.debug("Sandbox mode detected for request: {} {}",
                        httpRequest.getMethod(), httpRequest.getRequestURI());
                httpResponse.addHeader(SANDBOX_MODE_HEADER, "true");
            }
        }

        // Wrap the request to add sandbox headers
        SandboxHttpServletRequestWrapper wrappedRequest =
                new SandboxHttpServletRequestWrapper(httpRequest, isSandbox);

        chain.doFilter(wrappedRequest, httpResponse);
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
