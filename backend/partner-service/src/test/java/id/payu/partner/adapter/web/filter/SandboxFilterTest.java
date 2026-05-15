package id.payu.partner.adapter.web.filter;

import id.payu.partner.adapter.persistence.repository.ApiKeyRepository;
import id.payu.partner.adapter.persistence.entity.ApiKeyEntity;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SandboxFilter.
 */
@ExtendWith(MockitoExtension.class)
class SandboxFilterTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SandboxFilter sandboxFilter;

    @BeforeEach
    void setUp() {
        sandboxFilter = new SandboxFilter(apiKeyRepository);
    }

    @Test
    void doFilter_WithSandboxApiKey_AddsSandboxHeader() throws ServletException, IOException {
        // Given
        String apiKey = "payu_test_sandbox_key_12345";
        String keyHash = hashApiKey(apiKey);

        PartnerEntity partner = new PartnerEntity();
        partner.setId(1L);

        ApiKeyEntity sandboxKey = new ApiKeyEntity(
                partner, "payu_test_", keyHash, "2345",
                ApiKeyEntity.KeyEnvironment.SANDBOX, true
        );
        sandboxKey.setId(1L);

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(sandboxKey));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(sandboxKey);

        // When
        sandboxFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).addHeader("X-Sandbox-Mode", "true");
        verify(filterChain).doFilter(any(SandboxHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    void doFilter_WithProductionApiKey_NoSandboxHeader() throws ServletException, IOException {
        // Given
        String apiKey = "payu_live_production_key_12345";
        String keyHash = hashApiKey(apiKey);

        PartnerEntity partner = new PartnerEntity();
        partner.setId(1L);

        ApiKeyEntity productionKey = new ApiKeyEntity(
                partner, "payu_live_", keyHash, "2345",
                ApiKeyEntity.KeyEnvironment.LIVE, false
        );
        productionKey.setId(1L);

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(productionKey));

        // When
        sandboxFilter.doFilter(request, response, filterChain);

        // Then
        verify(response, never()).addHeader(eq("X-Sandbox-Mode"), anyString());
        verify(filterChain).doFilter(any(SandboxHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    void doFilter_WithNoApiKey_NoSandboxHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        sandboxFilter.doFilter(request, response, filterChain);

        // Then
        verify(response, never()).addHeader(eq("X-Sandbox-Mode"), anyString());
        verify(filterChain).doFilter(any(SandboxHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    void doFilter_WithInvalidApiKey_NoSandboxHeader() throws ServletException, IOException {
        // Given
        String apiKey = "invalid_key";
        String keyHash = hashApiKey(apiKey);

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.empty());

        // When
        sandboxFilter.doFilter(request, response, filterChain);

        // Then
        verify(response, never()).addHeader(eq("X-Sandbox-Mode"), anyString());
        verify(filterChain).doFilter(any(SandboxHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    void doFilter_WithBearerToken_ExtractsApiKey() throws ServletException, IOException {
        // Given
        String apiKey = "payu_test_sandbox_key_12345";
        String keyHash = hashApiKey(apiKey);

        PartnerEntity partner = new PartnerEntity();
        partner.setId(1L);

        ApiKeyEntity sandboxKey = new ApiKeyEntity(
                partner, "payu_test_", keyHash, "2345",
                ApiKeyEntity.KeyEnvironment.SANDBOX, true
        );
        sandboxKey.setId(1L);

        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);
        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(sandboxKey));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(sandboxKey);

        // When
        sandboxFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).addHeader("X-Sandbox-Mode", "true");
    }

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
}
