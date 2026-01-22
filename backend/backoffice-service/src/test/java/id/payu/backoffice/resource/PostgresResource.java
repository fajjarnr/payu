package id.payu.backoffice.resource;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

public class PostgresResource implements QuarkusTestResourceLifecycleManager {

    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("payu_backoffice")
            .withUsername("payu")
            .withPassword("payu123");

    @Override
    public Map<String, String> start() {
        db.start();
        return Map.of(
                "quarkus.datasource.jdbc.url", db.getJdbcUrl(),
                "quarkus.datasource.username", db.getUsername(),
                "quarkus.datasource.password", db.getPassword()
        );
    }

    @Override
    public void stop() {
        db.stop();
    }
}
