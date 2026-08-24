package id.payu.billing.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B3.3 RLS FORCE TDD proof: tenant A cannot see tenant B rows.
 * Runs real PostgreSQL via Testcontainers, applies Flyway (incl. V10 FORCE),
 * then proves fail-closed isolation with SET LOCAL app.tenant_id.
 */
@Testcontainers
@Tag("integration")
@DisplayName("Tenant isolation RLS (billing) — FORCE proof")
class TenantIsolationRlsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static Connection adminConn;
    private static final String TEST_USER = "payu_test_app";
    private static final String TEST_PASS = "test";

    private static Connection testUserConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), TEST_USER, TEST_PASS);
    }

    @BeforeAll
    static void migrate() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        adminConn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        adminConn.setAutoCommit(true);
        try (Statement s = adminConn.createStatement()) {
            s.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='" + TEST_USER + "') THEN CREATE ROLE " + TEST_USER + " WITH LOGIN PASSWORD '" + TEST_PASS + "' NOBYPASSRLS; END IF; END $$");
            s.execute("GRANT ALL ON ALL TABLES IN SCHEMA public TO " + TEST_USER);
            s.execute("GRANT USAGE ON SCHEMA public TO " + TEST_USER);
            s.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO " + TEST_USER);
        }
        insertViaAdmin("tenant-a", "BILL-A-1");
        insertViaAdmin("tenant-b", "BILL-B-1");
        insertViaAdmin("tenant-a", "BILL-A-2");
    }

    private static void insertViaAdmin(String tenant, String reference) throws Exception {
        String id = UUID.randomUUID().toString();
        try (PreparedStatement ps = adminConn.prepareStatement(
                "INSERT INTO bill_payments (id, tenant_id, account_id, reference_number, biller_type, customer_id, amount, total_amount, status, created_at) VALUES (?::uuid, ?, 'ACC-' || substr(?::text,1,8), ?, 'PLN', 'CUST-' || substr(?::text,1,8), 100.00, 100.00, 'PENDING', NOW())")) {
            ps.setString(1, id);
            ps.setString(2, tenant);
            ps.setString(3, id);
            ps.setString(4, reference);
            ps.setString(5, id);
            ps.executeUpdate();
        }
    }

    @AfterAll
    static void close() throws Exception {
        if (adminConn != null) adminConn.close();
    }

    private int countWithTenant(String tenant) throws Exception {
        try (Connection c = testUserConnection()) {
            c.setAutoCommit(false);
            if (tenant != null) {
                try (Statement s = c.createStatement()) {
                    s.execute("SET LOCAL app.tenant_id = '" + tenant + "'");
                }
            } else {
                try (Statement s = c.createStatement()) {
                    s.execute("RESET app.tenant_id");
                }
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM bill_payments")) {
                rs.next();
                int cnt = rs.getInt(1);
                c.commit();
                return cnt;
            }
        }
    }

    @Test
    @DisplayName("tenant-a sees only its own rows")
    void tenantASeesOnlyOwnRows() throws Exception {
        assertThat(countWithTenant("tenant-a")).isEqualTo(2);
    }

    @Test
    @DisplayName("tenant-b sees only its own rows")
    void tenantBSeesOnlyOwnRows() throws Exception {
        assertThat(countWithTenant("tenant-b")).isEqualTo(1);
    }

    @Test
    @DisplayName("no tenant (NULL) sees 0 rows — fail-closed")
    void noTenantSeesZero() throws Exception {
        assertThat(countWithTenant(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("SYSTEM sees all rows")
    void systemSeesAll() throws Exception {
        assertThat(countWithTenant("SYSTEM")).isEqualTo(3);
    }

    @Test
    @DisplayName("cross-tenant insert blocked by WITH CHECK")
    void crossTenantInsertBlocked() throws Exception {
        try (Connection c = testUserConnection()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.execute("SET LOCAL app.tenant_id = 'tenant-a'");
            }
            String id = UUID.randomUUID().toString();
            String dupRef = "DUP-" + UUID.randomUUID().toString().substring(0, 8);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO bill_payments (id, tenant_id, account_id, reference_number, biller_type, customer_id, amount, total_amount, status, created_at) VALUES (?::uuid, ?, 'ACC-X', ?, 'PLN', 'CUST-X', 50.00, 50.00, 'PENDING', NOW())")) {
                ps.setString(1, id);
                ps.setString(2, "tenant-b");
                ps.setString(3, dupRef);
                boolean threw = false;
                try {
                    ps.executeUpdate();
                    c.commit();
                } catch (SQLException e) {
                    threw = true;
                    assertThat(e.getMessage()).contains("row-level security");
                    c.rollback();
                }
                assertThat(threw).isTrue();
            }
        }
    }
}
