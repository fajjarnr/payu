package id.payu.partner;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Test profile that disables Kafka to avoid Hibernate Reactive checkpoint state store issues.
 *
 * This profile is used by default for all @QuarkusTest tests in partner-service.
 */
public class PartnerTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();

        // Disable Kafka at build time
        config.put("quarkus.kafka.enabled", "false");
        config.put("quarkus.messaging.enabled", "false");
        config.put("quarkus.reactive-messaging.enabled", "false");

        // Disable Kafka checkpointing
        config.put("smallrye.kafka.checkpoint.enabled", "false");
        config.put("smallrye.kafka.checkpoint.state-store", "none");

        // Disable messaging channels
        config.put("mp.messaging.incoming.enabled", "false");
        config.put("mp.messaging.outgoing.enabled", "false");

        return config;
    }
}
