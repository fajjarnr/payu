package id.payu.security.multitenancy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for multi-tenancy components.
 * <p>
 * Enabled by default. Disable with {@code payu.security.multitenancy.enabled=false}.
 * <p>
 * Registers:
 * <ul>
 *   <li>{@link TenantFilter} — extracts tenant from HTTP headers</li>
 *   <li>{@link TenantInterceptor} — enables Hibernate tenant filter (registered separately via @Component)</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "payu.security.multitenancy.enabled", havingValue = "true", matchIfMissing = true)
public class TenantConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "tenantFilter")
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }
}
