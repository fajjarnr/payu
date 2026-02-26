package id.payu.security.multitenancy;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.lang.annotation.*;

/**
 * Marks a JPA entity for automatic row-level tenant isolation.
 * <p>
 * Entities annotated with {@code @TenantAware} must have a {@code tenant_id} column.
 * When the Hibernate filter is enabled (via {@link TenantInterceptor}), only rows
 * matching the current tenant are returned by queries.
 * <p>
 * Usage:
 * <pre>
 * {@literal @}Entity
 * {@literal @}TenantAware
 * public class MyEntity {
 *     {@literal @}Column(name = "tenant_id")
 *     private String tenantId;
 * }
 * </pre>
 *
 * @see TenantContext
 * @see TenantInterceptor
 * @see TenantEntityListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@FilterDef(name = "tenantFilter",
           parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public @interface TenantAware {
}
