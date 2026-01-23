package id.payu.gateway.filter;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Filter to extract and propagate tenant ID for multitenancy support.
 * Tenant ID is extracted from X-Tenant-Id header or JWT claims.
 */
@Provider
@ApplicationScoped
public class TenantFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String DEFAULT_TENANT_ID = "default";
    private static final String TENANT_PROPERTY = "tenant-id";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String tenantId = extractTenantId(requestContext);

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = DEFAULT_TENANT_ID;
            Log.debugf("No tenant ID found, using default tenant");
        }

        requestContext.setProperty(TENANT_PROPERTY, tenantId);
        Log.debugf("Tenant ID: %s for request: %s %s",
                   tenantId,
                   requestContext.getMethod(),
                   requestContext.getUriInfo().getPath());
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        String tenantId = (String) requestContext.getProperty(TENANT_PROPERTY);
        if (tenantId != null) {
            responseContext.getHeaders().putSingle(TENANT_ID_HEADER, tenantId);
        }
    }

    private String extractTenantId(ContainerRequestContext requestContext) {
        String tenantId = requestContext.getHeaderString(TENANT_ID_HEADER);

        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }

        return null;
    }
}
