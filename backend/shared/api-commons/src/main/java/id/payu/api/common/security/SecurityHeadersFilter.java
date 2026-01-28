package id.payu.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security Headers Filter for PayU Digital Banking Platform.
 * Adds security headers to all HTTP responses to protect against common web vulnerabilities.
 *
 * Headers added:
 * - Strict-Transport-Security (HSTS): Enforces HTTPS connections
 * - X-Frame-Options: Prevents clickjacking attacks
 * - X-Content-Type-Options: Prevents MIME-sniffing
 * - Content-Security-Policy: Prevents XSS attacks
 * - X-XSS-Protection: Enables XSS filtering in browsers
 * - Referrer-Policy: Controls referrer information leakage
 * - Permissions-Policy: Controls browser features access
 *
 * PCI-DSS Compliance:
 * - Requirement 6.5.7: Improper access control (clickjacking)
 * - Requirement 6.5.1: Injection flaws (XSS protection)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String X_XSS_PROTECTION = "X-XSS-Protection";
    private static final String REFERRER_POLICY = "Referrer-Policy";
    private static final String PERMISSIONS_POLICY = "Permissions-Policy";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // HSTS: Enforce HTTPS for 1 year, include subdomains
        response.setHeader(STRICT_TRANSPORT_SECURITY, "max-age=31536000; includeSubDomains");

        // X-Frame-Options: Prevent clickjacking (DENY = no framing, SAMEORIGIN = allow same origin)
        // For REST APIs, DENY is appropriate since responses are not meant to be framed
        response.setHeader(X_FRAME_OPTIONS, "DENY");

        // X-Content-Type-Options: Prevent MIME-sniffing
        response.setHeader(X_CONTENT_TYPE_OPTIONS, "nosniff");

        // Content-Security-Policy: Restrict sources for content
        // For REST APIs with JSON responses, we only need to allow self
        response.setHeader(CONTENT_SECURITY_POLICY,
                "default-src 'self'; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'"
        );

        // X-XSS-Protection: Enable browser XSS filter (legacy, but still useful)
        response.setHeader(X_XSS_PROTECTION, "1; mode=block");

        // Referrer-Policy: Control referrer information
        response.setHeader(REFERRER_POLICY, "no-referrer");

        // Permissions-Policy: Restrict browser features
        response.setHeader(PERMISSIONS_POLICY,
                "geolocation=(), " +
                "microphone=(), " +
                "camera=(), " +
                "payment=(), " +
                "usb=(), " +
                "magnetometer=(), " +
                "gyroscope=(), " +
                "accelerometer=()"
        );

        // Cache-Control for sensitive endpoints (prevents caching of authenticated responses)
        if (isAuthenticatedRequest(request)) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines if the request is authenticated (has Authorization header).
     * Sensitive authenticated responses should not be cached.
     */
    private boolean isAuthenticatedRequest(HttpServletRequest request) {
        return request.getHeader("Authorization") != null;
    }
}
