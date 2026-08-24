package id.payu.compliance.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ADR-0063 / COMPLIANCE-HARDEN-001: the audit trail is append-only.
 *
 * Proves that the application DB role can INSERT + SELECT audit rows but
 * cannot UPDATE or DELETE them, once V4__revoke_update_delete_audit_tables.sql
 * has been applied by Flyway.
 *
 * The container's migrator role ("test") owns the tables. Before Flyway runs we
 * create the application role and grant it ALL on future tables (simulating ops
 * provisioning); V4 must then strip UPDATE and DELETE, leaving INSERT/SELECT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DisplayName("Audit trail immutability (ADR-0063 append-only)")
@Tag("integration")
class AuditTrailImmutabilityIntegrationTest {

    /** Matches the runtime app-role defaults, e.g. application-container.yml DB_USERNAME. */
    private static final String APP_ROLE = "payu_test";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    static void startContainerAndProvisionAppRole() throws SQLException {
        postgres.start();
        try (Connection c = ownerConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE ROLE " + APP_ROLE + " LOGIN PASSWORD '" + APP_ROLE + "'");
            // Provisioning baseline: app role starts with full DML on every table
            // Flyway (run as the container user) creates. The migration must strip
            // UPDATE/DELETE from the audit tables.
            s.execute("ALTER DEFAULT PRIVILEGES GRANT ALL ON TABLES TO " + APP_ROLE);
        }
    }

    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    private static Connection ownerConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static Connection appRoleConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), APP_ROLE, APP_ROLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"audit_reports", "compliance_checks", "data_access_audits"})
    @DisplayName("app role must NOT be able to UPDATE audit rows")
    void updateDenied(String table) {
        String sql = switch (table) {
            case "audit_reports" -> "UPDATE audit_reports SET merchant_id = 'tampered'";
            case "compliance_checks" -> "UPDATE compliance_checks SET status = 'PASS'";
            default -> "UPDATE data_access_audits SET user_id = 'tampered'";
        };
        assertThatThrownBy(() -> execAsAppRole(sql))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("permission denied");
    }

    @ParameterizedTest
    @ValueSource(strings = {"audit_reports", "compliance_checks", "data_access_audits"})
    @DisplayName("app role must NOT be able to DELETE audit rows")
    void deleteDenied(String table) {
        assertThatThrownBy(() -> execAsAppRole("DELETE FROM " + table))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("permission denied");
    }

    @Test
    @DisplayName("app role must still be able to INSERT and SELECT audit rows")
    void insertAndSelectAllowed() throws SQLException {
        UUID reportId = UUID.randomUUID();
        UUID accessId = UUID.randomUUID();

        try (Connection c = appRoleConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_reports (id, transaction_id, merchant_id, compliance_standard, "
                            + "overall_status, created_at, created_by) VALUES (?,?,?,?,?,?,?)")) {
                ps.setObject(1, reportId);
                ps.setObject(2, UUID.randomUUID());
                ps.setString(3, "MERCHANT_IMMUTABILITY_TEST");
                ps.setString(4, "AML");
                ps.setString(5, "PASS");
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(7, "it-self");
                assertThat(ps.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO data_access_audits (id, user_id, accessed_by, service_name, resource_type, "
                            + "operation_type, success, accessed_at, created_at) VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setObject(1, accessId);
                ps.setString(2, "user-1");
                ps.setString(3, "user-1");
                ps.setString(4, "compliance-service");
                ps.setString(5, "AUDIT_REPORT");
                ps.setString(6, "READ");
                ps.setBoolean(7, true);
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                assertThat(ps.executeUpdate()).isEqualTo(1);
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT (SELECT count(*) FROM audit_reports) AS reports, "
                                 + "(SELECT count(*) FROM data_access_audits) AS accesses")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt("reports")).isGreaterThanOrEqualTo(1);
                assertThat(rs.getInt("accesses")).isGreaterThanOrEqualTo(1);
            }
        }
    }

    private static void execAsAppRole(String sql) throws SQLException {
        try (Connection c = appRoleConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }
}
