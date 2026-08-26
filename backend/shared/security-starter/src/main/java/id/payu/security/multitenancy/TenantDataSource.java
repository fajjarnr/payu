package id.payu.security.multitenancy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * ADR-0033 RLS: binds {@code app.tenant_id} on every pooled connection before
 * its first statement so {@code FORCE ROW LEVEL SECURITY} policies evaluating
 * {@code current_setting('app.tenant_id', true)} see the request tenant.
 *
 * <p>Uses {@code SET LOCAL}: the binding lives for the current transaction only
 * and reverts automatically at commit/rollback, so pooled connections never
 * leak a tenant to the next borrower. Non-transactional (autocommit) usage is
 * skipped — RLS then fails closed, which is the intended posture for financial
 * tables.
 *
 * <p>WEB-RLS-001 (2026-08-26): the RLS migrations (V107+ in every service)
 * shipped without any production code path setting the GUC — as the app role,
 * every INSERT violated the policy (SQLState 42501) and every SELECT returned
 * zero rows. This decorator is the missing half. Binding happens at
 * first-statement time (not connection-acquisition time) because JPA
 * transaction managers acquire the connection before Spring marks the
 * transaction active.
 */
public final class TenantDataSource implements DataSource {

    private final DataSource delegate;

    public TenantDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return TenantConnection.proxy(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return TenantConnection.proxy(delegate.getConnection(username, password));
    }

    /** Expose the underlying pool (Hikari metrics/health checks unwrap through here). */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }

    @Override
    public java.io.PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    private static final class TenantConnection implements InvocationHandler {

        private final Connection delegate;
        private final AtomicBoolean bound = new AtomicBoolean();

        private TenantConnection(Connection delegate) {
            this.delegate = delegate;
        }

        static Connection proxy(Connection delegate) {
            return (Connection) Proxy.newProxyInstance(
                    TenantConnection.class.getClassLoader(),
                    new Class<?>[] { Connection.class },
                    new TenantConnection(delegate));
        }

        private void bindTenant() throws SQLException {
            if (!bound.compareAndSet(false, true)) {
                return;
            }
            if (delegate.getAutoCommit()) {
                // No transaction: SET LOCAL would degrade to session-level and
                // leak. Skip — RLS fails closed for non-transactional access.
                return;
            }
            String tenant = TenantContext.isSet() ? TenantContext.getTenantId() : TenantContext.DEFAULT_TENANT_ID;
            try (Statement st = delegate.createStatement()) {
                st.execute("SET LOCAL app.tenant_id = '" + tenant.replace("'", "''") + "'");
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.startsWith("prepareStatement") || name.startsWith("prepareCall") || name.startsWith("createStatement")) {
                bindTenant();
            }
            try {
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
