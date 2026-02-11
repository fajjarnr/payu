package id.payu.saga;

import id.payu.saga.config.SagaProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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
    // Configuration is handled by application-test.yml and SagaAutoConfiguration
}
