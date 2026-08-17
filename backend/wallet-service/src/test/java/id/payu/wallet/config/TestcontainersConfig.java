package id.payu.wallet.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 *
 * This configuration class provides Docker containers for integration testing:
 * - PostgreSQL 16 for database testing
 * - Kafka for event streaming testing
 *
 * Usage: Import this configuration in integration tests that need Docker containers.
 *
 * @author PayU Backend Team
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /**
     * PostgreSQL container for database integration tests.
     * The @ServiceConnection annotation enables Spring Boot to automatically
     * configure the datasource with the container's connection details.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("wallet_test")
                .withUsername("test")
                .withPassword("test");
    }

    /**
     * Kafka container for messaging integration tests.
     */
    @Bean
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    }
}
