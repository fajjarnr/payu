package id.payu.security.multitenancy;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * JPA entity listener that automatically sets the {@code tenantId} field
 * on entities annotated with {@link TenantAware}.
 * <p>
 * On {@code @PrePersist}, sets the tenantId from {@link TenantContext} if not already set.
 * On {@code @PreUpdate}, validates that the tenantId hasn't been changed
 * (prevents accidental cross-tenant data leakage).
 * <p>
 * Entities must have a field named {@code tenantId} (any access modifier).
 * <p>
 * Usage on entity:
 * <pre>
 * {@literal @}Entity
 * {@literal @}TenantAware
 * {@literal @}EntityListeners(TenantEntityListener.class)
 * public class MyEntity {
 *     {@literal @}Column(name = "tenant_id")
 *     private String tenantId;
 * }
 * </pre>
 */
public class TenantEntityListener {

    private static final Logger log = LoggerFactory.getLogger(TenantEntityListener.class);

    /**
     * Auto-populate tenantId on new entity creation.
     */
    @PrePersist
    public void setTenantOnCreate(Object entity) {
        if (!entity.getClass().isAnnotationPresent(TenantAware.class)) {
            return;
        }

        String currentTenant = TenantContext.getTenantId();
        Field tenantField = findTenantField(entity.getClass());
        if (tenantField == null) return;

        try {
            tenantField.setAccessible(true);
            Object existing = tenantField.get(entity);
            if (existing == null || existing.toString().isBlank()) {
                tenantField.set(entity, currentTenant);
                log.debug("Auto-set tenantId='{}' on {}", currentTenant,
                        entity.getClass().getSimpleName());
            }
        } catch (IllegalAccessException e) {
            log.error("Failed to set tenantId on {}: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Validate tenantId hasn't changed on update (prevent cross-tenant writes).
     */
    @PreUpdate
    public void validateTenantOnUpdate(Object entity) {
        if (!entity.getClass().isAnnotationPresent(TenantAware.class)) {
            return;
        }

        if (!TenantContext.isSet()) return;

        Field tenantField = findTenantField(entity.getClass());
        if (tenantField == null) return;

        try {
            tenantField.setAccessible(true);
            Object entityTenant = tenantField.get(entity);
            String currentTenant = TenantContext.getTenantId();

            if (entityTenant != null && !entityTenant.toString().equals(currentTenant)) {
                throw new SecurityException(
                        "Tenant mismatch: entity belongs to '" + entityTenant
                                + "' but current tenant is '" + currentTenant + "'");
            }
        } catch (IllegalAccessException e) {
            log.error("Failed to validate tenantId on {}: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    private Field findTenantField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField("tenantId");
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
