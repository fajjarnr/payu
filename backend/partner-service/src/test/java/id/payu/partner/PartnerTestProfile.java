package id.payu.partner;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Collections;
import java.util.Map;

/**
 * Test profile for Partner Service tests.
 *
 * Note: Kafka checkpointing creates a Hibernate Reactive persistence unit at build-time
 * which conflicts with blocking Hibernate ORM. The service's @Channel injections are
 * currently not working in tests due to this Quarkus limitation.
 *
 * Services that use @Channel Emitter (like SnapBiPaymentService) should be tested
 * separately with integration tests that include Kafka.
 */
public class PartnerTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // Return empty map - use default test configuration
        return Collections.emptyMap();
    }
}
