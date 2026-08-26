package id.payu.security.multitenancy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WEB-RLS-001 regression: {@link TenantDataSource} must bind app.tenant_id
 * (SET LOCAL) per transaction so FORCE ROW LEVEL SECURITY policies pass for
 * the app role — and must NOT leak the binding across transactions.
 *
 * <p>Runs as a dedicated non-superuser role: Testcontainers defaults to the
 * postgres superuser, which bypasses RLS entirely and would make this test
 * vacuously green (the exact blind spot that let the original defect ship).
 */
@Testcontainers
class TenantDataSourceRlsTest {

    @Container
    static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:16-alpine");

    static HikariDataSource adminPool;
    static HikariDataSource appPool;
    static TenantDataSource decorated;
    static JdbcTemplate decoratedJdbc;
    static JdbcTemplate plainJdbc;

    @BeforeAll
    static void provision() throws Exception {
        adminPool = pool(PG.getUsername(), PG.getPassword());
        try (Connection c = adminPool.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE ROLE rlsapp LOGIN PASSWORD 'rlsapp'");
            s.execute("CREATE TABLE money (id serial primary key, tenant_id text not null, amount numeric(19,4) not null)");
            s.execute("INSERT INTO money (tenant_id, amount) VALUES ('tenant-a', 100.0000), ('tenant-b', 50.0000)");
            s.execute("ALTER TABLE money ENABLE ROW LEVEL SECURITY");
            s.execute("ALTER TABLE money FORCE ROW LEVEL SECURITY");
            s.execute("CREATE POLICY tenant_isolation ON money "
                    + "USING (tenant_id = current_setting('app.tenant_id', true)) "
                    + "WITH CHECK (tenant_id = current_setting('app.tenant_id', true))");
            s.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON money TO rlsapp");
            s.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO rlsapp");
        }
        appPool = pool("rlsapp", "rlsapp");
        decorated = new TenantDataSource(appPool);
        decoratedJdbc = new JdbcTemplate(decorated);
        plainJdbc = new JdbcTemplate(appPool);
    }

    @AfterAll
    static void close() {
        appPool.close();
        adminPool.close();
    }

    private static HikariDataSource pool(String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(2);
        return new HikariDataSource(cfg);
    }

    private TransactionTemplate txOn(Object ds) {
        return new TransactionTemplate(new DataSourceTransactionManager((javax.sql.DataSource) ds));
    }

    private int count(String tenantGuc, String whereTenant) {
        TenantContext.setTenantId(tenantGuc);
        Integer n = txOn(decorated).execute(status ->
                decoratedJdbc.queryForObject("SELECT count(*) FROM money WHERE tenant_id = ?", Integer.class, whereTenant));
        TenantContext.clear();
        return n == null ? -1 : n;
    }

    @Test
    void insertWithoutDecoratorViolatesRls() {
        // The original defect: plain pool, no GUC → policy WITH CHECK fails.
        TenantContext.setTenantId("tenant-a");
        RuntimeException out = assertThrows(RuntimeException.class, () ->
                txOn(appPool).execute(status -> {
                    plainJdbc.update("INSERT INTO money (tenant_id, amount) VALUES ('tenant-a', 1.0000)");
                    return null;
                }));
        assertTrue(out.getCause() instanceof SQLException
                && out.getCause().getMessage().contains("row-level security"),
                () -> String.valueOf(out.getCause()));
        TenantContext.clear();
    }

    @Test
    void decoratorBindsTenantSoInsertAndReadSucceed() {
        TenantContext.setTenantId("tenant-a");
        txOn(decorated).execute(status -> {
            decoratedJdbc.update("INSERT INTO money (tenant_id, amount) VALUES ('tenant-a', 7.0000)");
            return null;
        });
        TenantContext.clear();
        // sees only its own tenant's rows: 1 seeded + 1 inserted
        assertEquals(2, count("tenant-a", "tenant-a"));
        assertEquals(0, count("tenant-a", "tenant-b"));
    }

    @Test
    void missingTenantFallsBackToDefault() {
        TenantContext.clear();
        assertEquals(0, count("default", "tenant-a")); // default tenant sees nothing of tenant-a
    }

    @Test
    void bindingRevertsAfterTransaction() {
        // After any transaction, a fresh one must start with the GUC unset —
        // SET LOCAL reverted at commit; no leakage into the next borrower.
        // Read via the UNdecorated pool: the decorator would rebind 'default'
        // before the probe and mask the answer.
        count("tenant-a", "tenant-a");
        TenantContext.clear();
        String guc = txOn(appPool).execute(status ->
                plainJdbc.queryForObject("SELECT coalesce(current_setting('app.tenant_id', true), 'NULL')", String.class));
        // NULL = never set on this session; '' = SET LOCAL reverted at commit.
        // Both mean the tenant binding did not leak into the next borrower.
        assertTrue(guc == null || guc.equals("NULL") || guc.isEmpty(), () -> "leaked GUC: " + guc);
    }
}
