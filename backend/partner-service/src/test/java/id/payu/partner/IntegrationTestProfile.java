package id.payu.partner;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile for integration tests that require Docker/Testcontainers.
 *
 * <p>This profile enables the full database stack using PostgreSQL Testcontainers
 * and should only be used when Docker is available. Tests using this profile will be
 * automatically skipped unless the {@code docker.enabled=true} system property is set.</p>
 *
 * <p><b>Migration Note:</b> This profile has been migrated from H2 to PostgreSQL Testcontainers
 * to provide better production parity and support for Hibernate Reactive datasource requirements.</p>
 *
 * <p>To use this profile, annotate your test class with:
 * <pre>
 * @QuarkusTest
 * @EnabledIfSystemProperty(named = "docker.enabled", matches = "true")
 * @QuarkusTestResource(value = id.payu.partner.test.resource.PostgresTestResource.class)
 * public class YourIntegrationTest { ... }
 * </pre></p>
 *
 * @see org.junit.jupiter.api.condition.EnabledIfSystemProperty
 * @see id.payu.partner.test.resource.PostgresTestResource
 */
public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // This profile now delegates to PostgresTestResource for datasource configuration.
        // The Testcontainers lifecycle manager provides the actual PostgreSQL connection details.
        // These settings are fallback values and will be overridden by PostgresTestResource.
        return Map.of(
                // Mark that we're running in integration test mode
                "quarkus.hibernate-orm.database.generation", "drop-and-create",
                "quarkus.hibernate-orm.packages", "id.payu.partner.domain",

                // Disable DevServices since Testcontainers manages the database
                "quarkus.datasource.devservices.enabled", "false",
                "quarkus.kafka.devservices.enabled", "false",

                // Enable messaging for integration tests
                "quarkus.kafka.enabled", "true",
                "quarkus.messaging.enabled", "true"
        );
    }

    @Override
    public String getConfigProfile() {
        return "integrationtest";
    }
}
