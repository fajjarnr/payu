package id.payu.security.multitenancy;

import java.util.Optional;

/**
 * Thread-local holder for the current tenant (partner) context.
 * <p>
 * Used by {@link TenantFilter} to propagate tenant identity from HTTP headers
 * to the application layer. All services should use this centralized implementation
 * instead of per-service copies.
 * <p>
 * Default tenant is "default" for backward compatibility with non-partner requests.
 *
 * @see TenantFilter
 * @see TenantAware
 */
public final class TenantContext {

    public static final String DEFAULT_TENANT_ID = "default";

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /**
     * Set the current tenant ID. Null or blank values default to "default".
     */
    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            CURRENT_TENANT.set(DEFAULT_TENANT_ID);
        } else {
            CURRENT_TENANT.set(tenantId.trim());
        }
    }

    /**
     * Get the current tenant ID. Returns "default" if not set.
     */
    public static String getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get()).orElse(DEFAULT_TENANT_ID);
    }

    /**
     * Check if a tenant context is explicitly set (not default).
     */
    public static boolean isSet() {
        String id = CURRENT_TENANT.get();
        return id != null && !DEFAULT_TENANT_ID.equals(id);
    }

    /**
     * Clear the tenant context (must be called in finally blocks).
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
