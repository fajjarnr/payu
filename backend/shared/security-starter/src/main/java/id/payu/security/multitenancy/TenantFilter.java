package id.payu.security.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring filter that extracts tenant ID from HTTP request headers
 * and sets it in {@link TenantContext}.
 * <p>
 * Header priority:
 * <ol>
 *   <li>{@code X-Tenant-Id} — explicit tenant header (forwarded by gateway)</li>
 *   <li>{@code X-Partner-Id} — partner identifier (from SNAP-BI or API key auth)</li>
 *   <li>Falls back to "default" for consumer (non-partner) requests</li>
 * </ol>
 * <p>
 * This filter runs early in the chain (ordered at {@link Ordered#HIGHEST_PRECEDENCE} + 10)
 * to ensure tenant context is available for all downstream processing.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String PARTNER_HEADER = "X-Partner-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = request.getHeader(PARTNER_HEADER);
        }

        TenantContext.setTenantId(tenantId);

        if (TenantContext.isSet()) {
            log.debug("Tenant context set: {}", TenantContext.getTenantId());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
