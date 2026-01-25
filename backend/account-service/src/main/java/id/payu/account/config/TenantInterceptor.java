package id.payu.account.config;

import id.payu.account.multitenancy.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@ConditionalOnBean(EntityManagerFactory.class)
public class TenantInterceptor {

    @PersistenceContext
    private EntityManager entityManager;

    public void executeInTenant(String tenantId, Runnable runnable) {
        String previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            runnable.run();
        } finally {
            if (previousTenant != null) {
                TenantContext.setTenantId(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    public void enableTenantFilter() {
        entityManager
            .unwrap(org.hibernate.Session.class)
            .enableFilter("tenantFilter")
            .setParameter("tenantId", TenantContext.getTenantId());
    }
}
