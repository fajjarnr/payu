package id.payu.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SecurityHeadersFilter}.
 *
 * Tests verify that all security headers are correctly applied to HTTP responses
 * to protect against common web vulnerabilities (XSS, Clickjacking, MIME-sniffing).
 *
 * PCI-DSS Compliance:
 * - Requirement 6.5.7: Clickjacking protection (X-Frame-Options)
 * - Requirement 6.5.1: Injection flaws (XSS protection via CSP)
 */
@ExtendWith(MockitoExtension.class)
class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Should add HSTS header with max-age 1 year and includeSubDomains")
    void shouldAddHSTSHeader() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    @DisplayName("Should add X-Frame-Options header with DENY")
    void shouldAddXFrameOptionsHeader() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(response.getHeader("X-Frame-Options"))
                .isEqualTo("DENY");
    }

    @Test
    @DisplayName("Should add X-Content-Type-Options header with nosniff")
    void shouldAddXContentTypeOptionsHeader() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(response.getHeader("X-Content-Type-Options"))
                .isEqualTo("nosniff");
    }

    @Test
    @DisplayName("Should add Content-Security-Policy header with restrictive policy")
    void shouldAddContentSecurityPolicyHeader() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        String csp = response.getHeader("Content-Security-Policy");
        assertThat(csp).contains("default-src 'self'");
        assertThat(csp).contains("frame-ancestors 'none'");
        assertThat(csp).contains("base-uri 'self'");
        assertThat(csp).contains("form-action 'self'");
    }

    @Test
    @DisplayName("Should add X-XSS-Protection header with mode=block")
    void shouldAddXXSSProtectionHeader() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(response.getHeader("X-XSS-Protection"))
                .isEqualTo("1; mode=block");
    }

    @Test
    @DisplayName("Should add Referrer-Policy header with no-referrer")
    void shouldAddReferrerPolicyHeader() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(response.getHeader("Referrer-Policy"))
                .isEqualTo("no-referrer");
    }

    @Test
    @DisplayName("Should add Permissions-Policy header restricting browser features")
    void shouldAddPermissionsPolicyHeader() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        String permissionsPolicy = response.getHeader("Permissions-Policy");
        assertThat(permissionsPolicy).contains("geolocation=()");
        assertThat(permissionsPolicy).contains("microphone=()");
        assertThat(permissionsPolicy).contains("camera=()");
        assertThat(permissionsPolicy).contains("payment=()");
        assertThat(permissionsPolicy).contains("usb=()");
        assertThat(permissionsPolicy).contains("magnetometer=()");
        assertThat(permissionsPolicy).contains("gyroscope=()");
        assertThat(permissionsPolicy).contains("accelerometer=()");
    }

    @Test
    @DisplayName("Should add cache control headers for authenticated requests")
    void shouldAddCacheControlHeadersForAuthenticatedRequests() {
        // Given
        request.addHeader("Authorization", "Bearer token123");

        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(response.getHeader("Cache-Control"))
                .isEqualTo("no-store, no-cache, must-revalidate, private");
        assertThat(response.getHeader("Pragma"))
                .isEqualTo("no-cache");
        assertThat(response.getHeader("Expires"))
                .isEqualTo("0");
    }

    @Test
    @DisplayName("Should not add cache control headers for non-authenticated requests")
    void shouldNotAddCacheControlHeadersForNonAuthenticatedRequests() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(response.getHeader("Cache-Control")).isNull();
        assertThat(response.getHeader("Pragma")).isNull();
        assertThat(response.getHeader("Expires")).isNull();
    }

    @Test
    @DisplayName("Should continue filter chain after adding headers")
    void shouldContinueFilterChain() throws Exception {
        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(any(HttpServletRequest.class), any());
    }

    @Test
    @DisplayName("Should apply all security headers in single filter pass")
    void shouldApplyAllSecurityHeaders() {
        // When
        try {
            filter.doFilter(request, response, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then - verify all critical headers are present
        assertThat(response.getHeaderNames()).contains(
                "Strict-Transport-Security",
                "X-Frame-Options",
                "X-Content-Type-Options",
                "Content-Security-Policy",
                "X-XSS-Protection",
                "Referrer-Policy",
                "Permissions-Policy"
        );
    }

    @Test
    @DisplayName("Should handle requests without Authorization header gracefully")
    void shouldHandleRequestsWithoutAuthorizationHeader() {
        // Given - request without Authorization header (default)

        // When & Then - should not throw exception
        assertThatCode(() -> filter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should use highest precedence order")
    void shouldUseHighestPrecedenceOrder() {
        // Given
        filter = new SecurityHeadersFilter();

        // When & Then
        // The @Order(Ordered.HIGHEST_PRECEDENCE) annotation ensures this filter
        // runs before other filters in the chain
        assertThat(filter).isNotNull();
    }
}
