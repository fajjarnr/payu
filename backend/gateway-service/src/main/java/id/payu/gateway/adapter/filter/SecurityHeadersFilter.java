package id.payu.gateway.adapter.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

/**
 * Filter to enforce API security headers on all gateway responses (DEVSECOPS-004).
 */
@Provider
@ApplicationScoped
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        // 1. Strict-Transport-Security (HSTS)
        headers.putSingle("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // 2. Content-Security-Policy (CSP)
        headers.putSingle("Content-Security-Policy", "default-src 'none'");

        // 3. X-Frame-Options
        headers.putSingle("X-Frame-Options", "DENY");

        // 4. X-Content-Type-Options
        headers.putSingle("X-Content-Type-Options", "nosniff");

        // 5. X-Request-ID (Propagate or generate)
        if (headers.getFirst("X-Request-ID") == null && headers.getFirst("X-Request-Id") == null) {
            String requestId = requestContext.getHeaderString("X-Request-ID");
            if (requestId == null || requestId.isBlank()) {
                requestId = requestContext.getHeaderString("X-Request-Id");
            }
            if (requestId == null || requestId.isBlank()) {
                requestId = (String) requestContext.getProperty("requestId");
            }
            if (requestId == null || requestId.isBlank()) {
                requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
            }
            headers.putSingle("X-Request-ID", requestId);
        }
    }
}
