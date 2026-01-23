package id.payu.account.multitenancy;

import java.util.Optional;

public class TenantContext {
    private static final String DEFAULT_TENANT_ID = "default";
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            CURRENT_TENANT.set(DEFAULT_TENANT_ID);
        } else {
            CURRENT_TENANT.set(tenantId);
        }
    }

    public static String getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get()).orElse(DEFAULT_TENANT_ID);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
