package id.payu.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for optimized DataSource with HikariCP.
 *
 * <p>Configuration example:</p>
 * <pre>
 * spring:
 *   datasource:
 *     primary:
 *       url: jdbc:postgresql://localhost:5432/payu
 *       username: payu
 *       password: payu123
 *     read-replica:
 *       enabled: true
 *       url: jdbc:postgresql://read-replica:5432/payu
 *       username: payu
 *       password: payu123
 *     pool:
 *       maximum-pool-size: 20
 *       minimum-idle: 10
 *       connection-timeout: 30s
 *       idle-timeout: 10m
 *       max-lifetime: 30m
 *       leak-detection-threshold: 60s
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {

    /**
     * Primary datasource configuration for write operations.
     */
    private Primary primary = new Primary();

    /**
     * Read replica datasource configuration for read operations.
     */
    private ReadReplica readReplica = new ReadReplica();

    /**
     * Connection pool configuration.
     */
    private Pool pool = new Pool();

    @Data
    public static class Primary {
        /**
         * JDBC URL for primary database.
         */
        private String url;

        /**
         * Database username.
         */
        private String username;

        /**
         * Database password.
         */
        private String password;

        /**
         * JDBC driver class name.
         */
        private String driverClassName = "org.postgresql.Driver";
    }

    @Data
    public static class ReadReplica {
        /**
         * Enable read replica datasource.
         */
        private boolean enabled = false;

        /**
         * JDBC URL for read replica database.
         */
        private String url;

        /**
         * Database username.
         */
        private String username;

        /**
         * Database password.
         */
        private String password;

        /**
         * JDBC driver class name.
         */
        private String driverClassName = "org.postgresql.Driver";

        /**
         * Maximum pool size for read replica (typically larger than primary).
         */
        private int maximumPoolSize = 30;

        /**
         * Minimum idle connections in pool.
         */
        private int minimumIdle = 15;

        /**
         * Connection timeout in milliseconds.
         */
        private long connectionTimeout = 30000;

        /**
         * Idle timeout for connections.
         */
        private Duration idleTimeout = Duration.ofMinutes(10);

        /**
         * Maximum lifetime of connections.
         */
        private Duration maxLifetime = Duration.ofMinutes(30);

        /**
         * Connection test query.
         */
        private String connectionTestQuery = "SELECT 1";

        /**
         * Validation timeout in milliseconds.
         */
        private long validationTimeout = 5000;

        /**
         * Enable auto-commit.
         */
        private boolean autoCommit = true;

        /**
         * Leak detection threshold in milliseconds (0 = disabled).
         */
        private long leakDetectionThreshold = 60000;
    }

    @Data
    public static class Pool {
        /**
         * Maximum pool size.
         * Formula: cores * 2 + effective_spindle_count
         */
        private int maximumPoolSize = 20;

        /**
         * Minimum idle connections in pool.
         */
        private int minimumIdle = 10;

        /**
         * Connection timeout in milliseconds.
         */
        private long connectionTimeout = 30000;

        /**
         * Idle timeout for connections.
         */
        private Duration idleTimeout = Duration.ofMinutes(10);

        /**
         * Maximum lifetime of connections.
         */
        private Duration maxLifetime = Duration.ofMinutes(30);

        /**
         * Connection test query.
         */
        private String connectionTestQuery = "SELECT 1";

        /**
         * Validation timeout in milliseconds.
         */
        private long validationTimeout = 5000;

        /**
         * Enable auto-commit.
         */
        private boolean autoCommit = true;

        /**
         * Leak detection threshold in milliseconds (0 = disabled).
         */
        private long leakDetectionThreshold = 60000;
    }
}
