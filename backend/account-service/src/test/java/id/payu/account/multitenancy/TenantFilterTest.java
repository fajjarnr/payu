package id.payu.account.multitenancy;

import id.payu.security.multitenancy.TenantContext;
import id.payu.security.multitenancy.TenantInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: multitenancy coverage (TenantFilter + TenantEnforcementAspect).
 */
@DisplayName("Account multitenancy")
class TenantFilterTest {

    @Test
    void derivesTenantFromJwtClaimAndClearsAfter() throws Exception {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("tenant_id", "partner-42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();
        TenantFilter filter = new TenantFilter();

        filter.doFilter(request, response, (req, res) -> seen.set(TenantContext.getTenantId()));

        assertThat(seen.get()).isEqualTo("partner-42");
        assertThat(TenantContext.getTenantId()).isEqualTo("default");
    }

    @Test
    void anonymousFallsBackToDefaultTenant() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();
        TenantFilter filter = new TenantFilter();

        filter.doFilter(request, response, (req, res) -> seen.set(TenantContext.getTenantId()));

        assertThat(seen.get()).isEqualTo("default");
    }

    @Test
    void jwtWithoutTenantClaimFallsBackToDefault() throws Exception {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").claim("sub", "user-1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();
        TenantFilter filter = new TenantFilter();

        filter.doFilter(request, response, (req, res) -> seen.set(TenantContext.getTenantId()));

        assertThat(seen.get()).isEqualTo("default");
    }

    @Test
    void aspectProceedsEvenWhenInterceptorFails() throws Throwable {
        @SuppressWarnings("unchecked")
        ObjectProvider<TenantInterceptor> provider = mock(ObjectProvider.class);
        TenantInterceptor interceptor = mock(TenantInterceptor.class);
        doThrow(new RuntimeException("no session")).when(interceptor).enableTenantFilter();
        when(provider.getIfAvailable()).thenReturn(interceptor);
        org.aspectj.lang.ProceedingJoinPoint pjp = mock(org.aspectj.lang.ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("ok");

        TenantEnforcementAspect aspect = new TenantEnforcementAspect(provider);
        Object result = aspect.enforceTenantFilter(pjp);

        assertThat(result).isEqualTo("ok");
    }
}
