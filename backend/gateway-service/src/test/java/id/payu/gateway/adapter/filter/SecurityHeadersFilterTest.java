package id.payu.gateway.adapter.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class SecurityHeadersFilterTest {

    @Test
    @DisplayName("should inject security headers on response filter invocation")
    void shouldInjectSecurityHeaders() {
        // Given
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
        ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);
        when(requestContext.getHeaderString("X-Request-ID")).thenReturn("test-req-id-123");

        // When
        filter.filter(requestContext, responseContext);

        // Then
        assertEquals("max-age=31536000; includeSubDomains; preload", headers.getFirst("Strict-Transport-Security"));
        assertEquals("default-src 'none'", headers.getFirst("Content-Security-Policy"));
        assertEquals("DENY", headers.getFirst("X-Frame-Options"));
        assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
        assertEquals("test-req-id-123", headers.getFirst("X-Request-ID"));
    }

    @Test
    @DisplayName("should generate X-Request-ID if not present in request context")
    void shouldGenerateRequestIdIfMissing() {
        // Given
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
        ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        // When
        filter.filter(requestContext, responseContext);

        // Then
        assertNotNull(headers.getFirst("X-Request-ID"));
        String generatedId = (String) headers.getFirst("X-Request-ID");
        assertEquals(8, generatedId.length());
    }
}
