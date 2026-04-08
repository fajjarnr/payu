package id.payu.gateway.adapter.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterClientIdTest {

    private final RateLimitFilter filter = new RateLimitFilter();

    @Test
    @DisplayName("should include user id for authenticated requests")
    void shouldIncludeUserIdForAuthenticatedRequests() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("10.0.0.5");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer test-token");
        when(requestContext.getHeaderString("X-User-Id")).thenReturn("user-123");

        assertEquals("10.0.0.5:user-123", filter.getClientId(requestContext));
    }

    @Test
    @DisplayName("should fall back to account id when user id is unavailable")
    void shouldFallbackToAccountId() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("10.0.0.5");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer test-token");
        when(requestContext.getHeaderString("X-User-Id")).thenReturn(null);
        when(requestContext.getHeaderString("X-Account-Id")).thenReturn("account-456");

        assertEquals("10.0.0.5:account-456", filter.getClientId(requestContext));
    }

    @Test
    @DisplayName("should retain authenticated fallback when identity headers are unavailable")
    void shouldFallbackToAuthenticatedSuffix() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("10.0.0.5");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer test-token");
        when(requestContext.getHeaderString("X-User-Id")).thenReturn(null);
        when(requestContext.getHeaderString("X-Account-Id")).thenReturn(null);

        assertEquals("10.0.0.5:authenticated", filter.getClientId(requestContext));
    }
}