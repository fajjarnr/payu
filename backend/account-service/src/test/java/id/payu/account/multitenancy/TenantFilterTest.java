package id.payu.account.multitenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import id.payu.security.multitenancy.TenantContext;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ACCOUNT-003: the tenant must come from a trusted credential (the signed JWT),
 * never from the client-controlled X-Tenant-Id header.
 */
@DisplayName("TenantFilter")
class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("uses the tenant_id claim from the authenticated JWT")
    void tenantFromJwtClaim() throws Exception {
        authenticateWithClaim("tenant-42");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "attacker-chosen-tenant");

        String tenantDuringRequest = captureTenantDuringRequest(request);

        assertThat(tenantDuringRequest).isEqualTo("tenant-42");
    }

    @Test
    @DisplayName("ignores the X-Tenant-Id header even when the JWT has no tenant claim")
    void headerIgnoredWithoutClaim() throws Exception {
        authenticateWithClaim(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "attacker-chosen-tenant");

        String tenantDuringRequest = captureTenantDuringRequest(request);

        assertThat(tenantDuringRequest).isEqualTo("default");
    }

    @Test
    @DisplayName("anonymous requests fall back to the default tenant")
    void anonymousUsesDefaultTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "attacker-chosen-tenant");

        String tenantDuringRequest = captureTenantDuringRequest(request);

        assertThat(tenantDuringRequest).isEqualTo("default");
    }

    @Test
    @DisplayName("clears the context after the request")
    void clearsContextAfterRequest() throws Exception {
        authenticateWithClaim("tenant-42");
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(TenantContext.getTenantId()).isEqualTo("default");
    }

    private String captureTenantDuringRequest(MockHttpServletRequest request) throws Exception {
        String[] captured = new String[1];
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                captured[0] = TenantContext.getTenantId();
            }
        });
        return captured[0];
    }

    private void authenticateWithClaim(String tenantId) {
        Map<String, Object> claims = new java.util.HashMap<>(Map.of("sub", "user-1"));
        if (tenantId != null) {
            claims.put("tenant_id", tenantId);
        }
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "none"), claims);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
