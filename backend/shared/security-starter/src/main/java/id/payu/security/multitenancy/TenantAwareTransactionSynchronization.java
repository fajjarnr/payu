package id.payu.security.multitenancy;

import org.springframework.transaction.support.TransactionSynchronization;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * ADR-0033 RLS: SET LOCAL app.tenant_id on transaction start, RESET on close.
 * ponytail: per-transaction GUC, not per-statement
 */
public class TenantAwareTransactionSynchronization implements TransactionSynchronization {
    private final DataSource dataSource;
    public TenantAwareTransactionSynchronization(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public void afterCompletion(int status) {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) { stmt.execute("RESET app.tenant_id"); }
            conn.close();
        } catch (Exception ignored) {} finally { TenantContext.clear(); }
    }
}
