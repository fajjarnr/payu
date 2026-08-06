package id.payu.security.multitenancy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TenantConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TenantConfiguration.class));

    @Test
    void backsOffWhenServiceDefinesTenantFilter() {
        contextRunner.withUserConfiguration(ServiceTenantConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("tenantFilter")).isSameAs(ServiceTenantConfiguration.FILTER);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ServiceTenantConfiguration {
        private static final Object FILTER = new Object();

        @Bean("tenantFilter")
        Object tenantFilter() {
            return FILTER;
        }
    }
}
