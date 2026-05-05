package id.payu.saga;

import id.payu.saga.config.SagaProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for saga-starter integration tests.
 * Configures H2 in-memory database and enables saga infrastructure.
 *
 * Note: EntityScan and EnableJpaRepositories are handled by SagaAutoConfiguration.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(SagaProperties.class)
public class TestConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
