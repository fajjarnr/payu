package id.payu.backoffice.resource;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

public class PostgresResource implements QuarkusTestResourceLifecycleManager {

    private static final String DOCKER_ENABLED_PROPERTY = "docker.enabled";
    static PostgreSQLContainer<?> db;

    @Override
    public Map<String, String> start() {
        // Skip container startup if Docker is not explicitly enabled
        if (!isDockerEnabled()) {
            throw new IllegalStateException(
                "Docker is not enabled. This test requires Docker.\n" +
                "To run tests with Docker, use: mvn test -Ddocker.enabled=true"
            );
        }

        db = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("payu_backoffice")
            .withUsername("payu")
            .withPassword("payu123");

        db.start();
        return Map.of(
                "quarkus.datasource.jdbc.url", db.getJdbcUrl(),
                "quarkus.datasource.username", db.getUsername(),
                "quarkus.datasource.password", db.getPassword()
        );
    }

    @Override
    public void stop() {
        if (db != null && db.isRunning()) {
            db.stop();
        }
    }

    /**
     * Check if Docker is enabled via system property.
     */
    private boolean isDockerEnabled() {
        return "true".equals(System.getProperty(DOCKER_ENABLED_PROPERTY));
    }
}
