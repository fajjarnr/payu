package id.payu.account.multitenancy;

import id.payu.security.multitenancy.TenantInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Enforces the Hibernate {@code tenantFilter} on every persistence access
 * (ACCOUNT-003). Entities are annotated {@link id.payu.security.multitenancy.TenantAware}
 * but the filter is only active after {@code enableFilter()} is called on the
 * current session — previously nothing ever enabled it, so the tenant_id column
 * never constrained reads. Running it at the adapter boundary covers every
 * repository read and write inside the active transaction.
 */
@Aspect
@Component
public class TenantEnforcementAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantEnforcementAspect.class);

    private final ObjectProvider<TenantInterceptor> tenantInterceptorProvider;

    public TenantEnforcementAspect(ObjectProvider<TenantInterceptor> tenantInterceptorProvider) {
        this.tenantInterceptorProvider = tenantInterceptorProvider;
    }

    @Around("within(id.payu.account.adapter.persistence..*)")
    public Object enforceTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        TenantInterceptor tenantInterceptor = tenantInterceptorProvider.getIfAvailable();
        if (tenantInterceptor != null) {
            try {
                tenantInterceptor.enableTenantFilter();
            } catch (Exception e) {
                log.debug("No active session to enable tenant filter: {}", e.getMessage());
            }
        }
        return joinPoint.proceed();
    }
}
