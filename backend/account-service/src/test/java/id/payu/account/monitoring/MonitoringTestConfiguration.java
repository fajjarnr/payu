package id.payu.account.monitoring;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Minimal test configuration for monitoring tests.
 * Only loads actuator and web components required for actuator endpoint testing.
 * Excludes database, security, Kafka, and cache-starter auto-configurations.
 * Configures Prometheus metrics registry for testing.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(
    exclude = {
        org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
        org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration.class,
        org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
        org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration.class,
        id.payu.cache.config.CacheAutoConfiguration.class,
        id.payu.resilience.config.ResilienceAutoConfiguration.class,
        id.payu.security.config.SecurityAutoConfiguration.class
    }
)
public class MonitoringTestConfiguration {

    /**
     * Configure Prometheus meter registry for testing.
     * This ensures the Prometheus endpoint is available during tests.
     * Uses SIMPLE config for Prometheus to avoid dependency on Prometheus client.
     */
    @Bean
    @ConditionalOnMissingBean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    /**
     * Configure MICROMETER clock for testing.
     * Required for PrometheusMeterRegistry to work properly.
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock micrometerClock() {
        return Clock.SYSTEM;
    }
}


