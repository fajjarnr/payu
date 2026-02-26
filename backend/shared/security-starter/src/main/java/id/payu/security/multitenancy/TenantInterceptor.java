package id.payu.security.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Enables and manages the Hibernate tenant filter on the current session.
 * <p>
 * Call {@link #enableTenantFilter()} at the start of a request (typically via AOP
 * or manually) to activate row-level filtering for {@link TenantAware} entities.
 * <p>
 * Also provides {@link #executeInTenant(String, Runnable)} for scoped tenant switches
 * (e.g., admin operations across tenants).
 *
 * @see TenantAware
 * @see TenantContext
 */
@Component("tenantInterceptor")
@ConditionalOnBean(EntityManagerFactory.class)
public class TenantInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Enable the Hibernate tenant filter for the current session.
     * Must be called after {@link TenantContext#setTenantId(String)}.
     */
    public void enableTenantFilter() {
        String tenantId = TenantContext.getTenantId();
        entityManager.unwrap(Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);
        log.debug("Tenant filter enabled for tenant: {}", tenantId);
    }

    /**
     * Execute a runnable in the context of a specific tenant.
     * Restores the previous tenant after execution.
     */
    public void executeInTenant(String tenantId, Runnable task) {
        String previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            enableTenantFilter();
            task.run();
        } finally {
            if (previousTenant != null) {
                TenantContext.setTenantId(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }
}
