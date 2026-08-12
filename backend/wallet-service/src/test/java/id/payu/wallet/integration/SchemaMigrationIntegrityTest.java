package id.payu.wallet.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * WALLET-002: app must boot against a FRESH database created only by Flyway
 * migrations, with Hibernate validate enabled.
 *
 * Regression: V10 created split_recipients.type / split_payment_legs.credited_at
 * while entities map recipient_type / settled_at — hand-patched live DBs hid the
 * gap; fresh installs fail schema validation.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "payu.grpc.server.enabled=false"
})
class SchemaMigrationIntegrityTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wallet_migration_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.hikari.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.primary.hikari.username", POSTGRES::getUsername);
        registry.add("spring.datasource.primary.hikari.password", POSTGRES::getPassword);
        registry.add("spring.datasource.primary.hikari.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    void contextLoadsWithFreshMigrations() {
        // context boot = Flyway applied V1..V112 + Hibernate validate passed
    }
}
