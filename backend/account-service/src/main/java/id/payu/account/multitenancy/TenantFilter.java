package id.payu.account.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import id.payu.security.multitenancy.TenantContext;

import java.io.IOException;

/**
 * Derives the tenant from a trusted credential (ACCOUNT-003): the signed JWT's
 * {@code tenant_id} claim, not the client-controlled {@code X-Tenant-Id} header
 * (which is ignored). Anonymous requests fall back to the default tenant.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_ID_CLAIM = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        TenantContext.setTenantId(resolveTenant());

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);
            if (tenantId != null && !tenantId.isBlank()) {
                return tenantId;
            }
        }
        return "default";
    }
}
