package id.payu.notification.integration;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Test profile for Kafka integration tests.
 *
 * This profile configures Quarkus to use the Testcontainers Kafka instance
 * instead of a local Kafka broker. It disables unnecessary features to speed
 * up test execution.
 *
 * @author PayU Backend Team
 */
public class KafkaIntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();

        // Kafka configuration will be set dynamically by Testcontainers
        // The bootstrap servers will be injected via system properties
        config.put("kafka.bootstrap.servers", "${kafka.bootstrap.servers}");

        // Enable Kafka for integration tests
        config.put("quarkus.kafka.enabled", "true");

        // Use H2 for fast in-memory database during integration tests
        config.put("quarkus.datasource.db-kind", "h2");
        config.put("quarkus.datasource.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        config.put("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create");

        // Use mock mailer for integration tests
        config.put("quarkus.mailer.mock", "true");

        // Disable OpenTelemetry for integration tests
        config.put("quarkus.opentelemetry.enabled", "false");

        // Log level adjustments for debugging
        config.put("quarkus.log.category.\"id.payu.notification\".level", "DEBUG");
        config.put("quarkus.log.category.\"org.apache.kafka\".level", "INFO");

        // Configure messaging channels
        config.put("mp.messaging.incoming.wallet-events.topic", "wallet.balance.changed");
        config.put("mp.messaging.incoming.transaction-events.topic", "transaction.completed");
        config.put("mp.messaging.incoming.payment-events.topic", "payment-events");
        config.put("mp.messaging.incoming.split-bill-events.topic", "split-bill-events");

        return config;
    }

    @Override
    public String getConfigProfile() {
        return "kafka-integration-test";
    }
}
