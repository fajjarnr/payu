package id.payu.partner.adapter.web.filter;

import id.payu.partner.adapter.persistence.entity.ApiKeyEntity;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.application.service.ApiKeyService;
import id.payu.partner.domain.KeyEnvironment;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SandboxFilter — PARTNER-005: API key validation is delegated to
 * {@link ApiKeyService#validateKey} (the single production caller), and an
 * invalid/revoked key fails closed with 401.
 */
@ExtendWith(MockitoExtension.class)
class SandboxFilterTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SandboxFilter sandboxFilter;

    @BeforeEach
    void setUp() {
        sandboxFilter = new SandboxFilter(apiKeyService);
    }

    @Test
    void doFilter_WithSandboxApiKey_AddsSandboxHeader() throws ServletException, IOException {
        String apiKey = "payu_test_sandbox_key_12345";

        PartnerEntity partner = new PartnerEntity();
        partner.setId(1L);

        ApiKeyEntity sandboxKey = new ApiKeyEntity(
                partner, "payu_test_", "hash", "2345",
                KeyEnvironment.SANDBOX, true
        );
        sandboxKey.setId(1L);

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.validateKey(apiKey)).thenReturn(sandboxKey);

        sandboxFilter.doFilter(request, response, filterChain);

        verify(response).addHeader("X-Sandbox-Mode", "true");
        verify(filterChain).doFilter(any(SandboxHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    void doFilter_WithProductionApiKey_NoSandboxHeader() throws ServletException, IOException {
        String apiKey = "payu_live_production_key_12345";

        PartnerEntity partner = new PartnerEntity();
        partner.setId(1L);

        ApiKeyEntity productionKey = new ApiKeyEntity(
                partner, "payu_live_", "hash", "2345",
                KeyEnvironment.LIVE, false
        );
        productionKey.setId(1L);

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.validateKey(apiKey)).thenReturn(productionKey);

        sandboxFilter.doFilter(request, response, filterChain);

        verify(response, never()).addHeader(eq("X-Sandbox-Mode"), anyString());
        verify(filterChain).doFilter(any(SandboxHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    void doFilter_WithNoApiKey_NoSandboxHeader() throws ServletException, IOException {
        when(request.getHeader("X-API-Key")).thenReturn(null);

        sandboxFilter.doFilter(request, response, filterChain);

        verify(response, never()).addHeader(eq("X-Sandbox-Mode"), anyString());
        verify(filterChain).doFilter(any(SandboxHttpServletRequestWrapper.class), eq(response));
        verify(apiKeyService, never()).validateKey(any());
    }

    @Test
    void doFilter_WithInvalidApiKey_RejectsWith401() throws ServletException, IOException {
        String apiKey = "invalid_key";
        StringWriter body = new StringWriter();

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.validateKey(apiKey)).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        sandboxFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
        org.junit.jupiter.api.Assertions.assertTrue(
                body.toString().contains("Invalid or revoked API key"),
                "401 body should explain the failure");
    }

    @Test
    void doFilter_WithRevokedApiKey_RejectsWith401() throws ServletException, IOException {
        String apiKey = "payu_live_revoked";
        StringWriter body = new StringWriter();

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.validateKey(apiKey)).thenReturn(null); // revoked -> unusable
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        sandboxFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_WithBearerSnapBiToken_DoesNotTreatAsApiKey() throws ServletException, IOException {
        // SNAP-BI bearer tokens are authenticated via client-key HMAC at the
        // controller; the filter must not try to validate them as API keys.
        when(request.getHeader("X-API-Key")).thenReturn(null);

        sandboxFilter.doFilter(request, response, filterChain);

        verify(apiKeyService, never()).validateKey(any());
        verify(filterChain).doFilter(any(SandboxHttpServletRequestWrapper.class), eq(response));
    }
}
