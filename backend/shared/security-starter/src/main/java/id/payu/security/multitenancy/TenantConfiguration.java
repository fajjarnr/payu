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

    /**
     * WEB-RLS-001: wraps every {@link DataSource} so each transaction binds
     * {@code app.tenant_id} (SET LOCAL) before its first statement — without
     * this, FORCE ROW LEVEL SECURITY policies evaluate the GUC as NULL and
     * reject every write / hide every row from the app role. Static bean:
     * BeanPostProcessors must not depend on other beans.
     */
    @Bean
    public static org.springframework.beans.factory.config.BeanPostProcessor tenantDataSourceDecorator() {
        return new org.springframework.beans.factory.config.BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof javax.sql.DataSource ds && !(bean instanceof TenantDataSource)) {
                    return new TenantDataSource(ds);
                }
                return bean;
            }
        };
    }
}
