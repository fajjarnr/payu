package id.payu.wallet.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariDataSource;
import id.payu.wallet.application.service.WalletService;
import java.math.BigDecimal;

/**
 * WALLET-001: a transactional wallet path must never run against a pool with
 * autoCommit=true — PostgreSQL rejects commit with "Cannot commit when
 * autoCommit is true", and hibernate.connection.provider_disables_autocommit
 * tells Hibernate to rely on the pool instead of fixing auto-commit itself.
 * Regression: the datasource-starter pool (spring.datasource.primary.hikari.*)
 * must bind auto-commit: false in every profile.
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
class TransactionalPoolAutoCommitTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wallet_autocommit_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.hikari.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.primary.hikari.username", POSTGRES::getUsername);
        registry.add("spring.datasource.primary.hikari.password", POSTGRES::getPassword);
        registry.add("spring.datasource.primary.hikari.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    WalletService walletService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    HikariDataSource dataSource;

    @BeforeEach
    void seedWallet() {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.update(
                "INSERT INTO wallets (id, account_id, balance, reserved_balance, currency, status, version) "
                        + "VALUES (gen_random_uuid(), 'autocommit-test-account', 100.0000, 0.0000, 'IDR', 'ACTIVE', 0) "
                        + "ON CONFLICT (account_id) DO NOTHING"));
    }

    @Test
    void poolDisablesAutoCommit() throws Exception {
        try (var connection = dataSource.getConnection()) {
            org.assertj.core.api.Assertions.assertThat(connection.getAutoCommit()).isFalse();
        }
    }

    @Test
    void transactionalReadCommitsWithoutAutoCommitError() {
        BigDecimal balance = walletService.getAvailableBalance("autocommit-test-account");
        org.assertj.core.api.Assertions.assertThat(balance)
                .isEqualByComparingTo("100.0000");
    }
}
