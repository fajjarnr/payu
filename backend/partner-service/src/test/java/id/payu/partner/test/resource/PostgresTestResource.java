package id.payu.partner.test.resource;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Testcontainers PostgreSQL resource for integration tests.
 *
 * <p>This resource starts a PostgreSQL container for integration tests,
 * providing a real database environment that matches production.</p>
 *
 * <p>Usage: Annotate your test class with:
 * <pre>@QuarkusTestResource(value = PostgresTestResource.class)</pre></p>
 *
 * <p><b>Note:</b> Hibernate Reactive requires a reactive datasource.
 * For checkpointing, we use a separate named datasource with JDBC.</p>
 *
 * @author PayU Backend Team
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String DATABASE_NAME = "partner_test";
    private static final String DATABASE_USERNAME = "test";
    private static final String DATABASE_PASSWORD = "test";  // pragma: allowlist secret (test-only password)

    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD)
            .withReuse(true);

    @Override
    public Map<String, String> start() {
        db.start();

        // Primary datasource (JDBC for Hibernate ORM)
        // Note: For Hibernate Reactive, Quarkus would need quarkus.datasource.reactive.url
        // But since partner-service uses Hibernate ORM (not Reactive), we use JDBC URL
        Map<String, String> config = new java.util.HashMap<>();

        // Primary datasource configuration
        config.put("quarkus.datasource.db-kind", "postgresql");
        config.put("quarkus.datasource.jdbc.url", db.getJdbcUrl());
        config.put("quarkus.datasource.username", db.getUsername());
        config.put("quarkus.datasource.password", db.getPassword());

        // Named datasource for Kafka checkpointing "database" persistence unit
        config.put("quarkus.datasource.database.db-kind", "postgresql");
        config.put("quarkus.datasource.database.jdbc.url", db.getJdbcUrl());
        config.put("quarkus.datasource.database.username", db.getUsername());
        config.put("quarkus.datasource.database.password", db.getPassword());

        // Hibernate ORM configuration
        config.put("quarkus.hibernate-orm.database.generation", "drop-and-create");
        config.put("quarkus.hibernate-orm.database.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.put("quarkus.hibernate-orm.packages", "id.payu.partner.domain");

        // Named persistence unit for Kafka checkpointing
        config.put("quarkus.hibernate-orm.database.database.generation", "drop-and-create");
        config.put("quarkus.hibernate-orm.database.database.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.put("quarkus.hibernate-orm.database.datasource", "database");

        // Disable DevServices since we're using Testcontainers
        config.put("quarkus.datasource.devservices.enabled", "false");
        config.put("quarkus.kafka.devservices.enabled", "false");

        return config;
    }

    @Override
    public void stop() {
        db.stop();
    }
}
