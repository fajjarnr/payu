package id.payu.backoffice.testutil;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

/**
 * Testcontainers lifecycle manager for PostgreSQL integration tests.
 * <p>
 * This class manages a PostgreSQL container for integration tests.
 * The container is started once before all tests and shared across test classes.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @QuarkusTest
 * @QuarkusTestResource(PostgreSQLResourceTestLifecycleManager.class)
 * class MyIntegrationTest {
 *     // Test methods using PostgreSQL
 * }
 * }
 * </pre>
 * <p>
 * Requirements:
 * - Docker must be running
 * - Tests must be run with -Ddocker.enabled=true
 */
public class PostgreSQLResourceTestLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private static final String DOCKER_ENABLED_PROPERTY = "docker.enabled";
    private static PostgreSQLContainer<?> postgres;

    @Override
    public void init(Map<String, String> initArgs) {
        // No initialization arguments needed
    }

    @Override
    public Map<String, String> start() {
        // Skip container startup if Docker is not explicitly enabled
        if (!isDockerEnabled()) {
            throw new IllegalStateException(
                "Docker is not enabled. Integration tests require Docker.\n" +
                "To run integration tests, use: ./mvnw test -Ddocker.enabled=true"
            );
        }

        // Start PostgreSQL container
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("backoffice_test")
                .withUsername("test")
                .withPassword("test")
                .withExposedPorts(5432)
                .withReuse(true);

        postgres.start();

        // Return datasource configuration for Quarkus
        return Map.of(
            "quarkus.datasource.jdbc.url", postgres.getJdbcUrl(),
            "quarkus.datasource.username", postgres.getUsername(),
            "quarkus.datasource.password", postgres.getPassword()
        );
    }

    @Override
    public void stop() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    /**
     * Check if Docker is enabled via system property.
     * <p>
     * Tests can be run with Docker enabled using:
     * ./mvnw test -Ddocker.enabled=true
     *
     * @return true if docker.enabled system property is set to "true"
     */
    private boolean isDockerEnabled() {
        return "true".equals(System.getProperty(DOCKER_ENABLED_PROPERTY));
    }
}
