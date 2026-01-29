package id.payu.partner;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile for integration tests that require Docker.
 *
 * <p>This profile enables the full database stack and should only be used
 * when Docker is available. Tests using this profile will be automatically
 * skipped unless the {@code docker.enabled=true} system property is set.</p>
 *
 * <p><b>Note:</b> Currently uses H2 in-memory database for simplicity.
 * To use Testcontainers with PostgreSQL, add Testcontainers dependency to pom.xml
 * and update the datasource URL accordingly.</p>
 *
 * @see org.junit.jupiter.api.condition.EnabledIfSystemProperty
 */
public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.datasource.db-kind", "h2",
            "quarkus.datasource.jdbc.url", "jdbc:h2:mem:integrationtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            "quarkus.datasource.username", "sa",
            "quarkus.datasource.password", "",
            "quarkus.hibernate-orm.database.generation", "drop-and-create",
            "quarkus.hibernate-orm.packages", "id.payu.partner.domain",
            "quarkus.hibernate-orm.dialect", "org.hibernate.dialect.H2Dialect",
            "quarkus.kafka.enabled", "false",
            "quarkus.kafka.devservices.enabled", "false",
            "quarkus.messaging.enabled", "false"
        );
    }
}
