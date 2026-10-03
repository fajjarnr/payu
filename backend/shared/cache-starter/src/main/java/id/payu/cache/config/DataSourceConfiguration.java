package id.payu.cache.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import id.payu.cache.properties.DataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Configuration for optimized DataSource with HikariCP connection pooling.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Primary datasource for write operations</li>
 *   <li>Read replica datasource for read operations</li>
 *   <li>Tuned HikariCP connection pool settings</li>
 *   <li>Connection validation and leak detection</li>
 * </ul>
 *
 * <p>Disabled in container profile to allow Spring Boot auto-configuration.</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnClass(HikariDataSource.class)
@Profile("!container")
public class DataSourceConfiguration {

    /**
     * Primary datasource for write operations.
     * Disabled in container profile to allow Spring Boot auto-configuration.
     */
    @Bean
    @Primary
    public DataSource primaryDataSource(DataSourceProperties properties) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(properties.getPrimary().getUrl());
        config.setUsername(properties.getPrimary().getUsername());
        config.setPassword(properties.getPrimary().getPassword());
        config.setDriverClassName(properties.getPrimary().getDriverClassName());

        config.setMaximumPoolSize(properties.getPool().getMaximumPoolSize());
        config.setMinimumIdle(properties.getPool().getMinimumIdle());
        config.setConnectionTimeout(properties.getPool().getConnectionTimeout());
        config.setIdleTimeout((int) properties.getPool().getIdleTimeout().toMillis());
        config.setMaxLifetime((int) properties.getPool().getMaxLifetime().toMillis());

        config.setConnectionTestQuery(properties.getPool().getConnectionTestQuery());
        config.setValidationTimeout(properties.getPool().getValidationTimeout());

        config.setAutoCommit(properties.getPool().isAutoCommit());
        config.setReadOnly(false);

        if (properties.getPool().getLeakDetectionThreshold() > 0) {
            config.setLeakDetectionThreshold(properties.getPool().getLeakDetectionThreshold());
        }

        config.setPoolName("primary-pool");

        log.info("Configured primary datasource: url={}, maxPoolSize={}, minIdle={}",
            config.getJdbcUrl(), config.getMaximumPoolSize(), config.getMinimumIdle());

        return new HikariDataSource(config);
    }

    @Bean
    @ConditionalOnClass(HikariDataSource.class)
    public DataSource readReplicaDataSource(DataSourceProperties properties) {
        if (!properties.getReadReplica().isEnabled()) {
            log.info("Read replica is disabled");
            return null;
        }

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(properties.getReadReplica().getUrl());
        config.setUsername(properties.getReadReplica().getUsername());
        config.setPassword(properties.getReadReplica().getPassword());
        config.setDriverClassName(properties.getReadReplica().getDriverClassName());

        config.setMaximumPoolSize(properties.getReadReplica().getMaximumPoolSize());
        config.setMinimumIdle(properties.getReadReplica().getMinimumIdle());
        config.setConnectionTimeout(properties.getReadReplica().getConnectionTimeout());
        config.setIdleTimeout((int) properties.getReadReplica().getIdleTimeout().toMillis());
        config.setMaxLifetime((int) properties.getReadReplica().getMaxLifetime().toMillis());

        config.setConnectionTestQuery(properties.getReadReplica().getConnectionTestQuery());
        config.setValidationTimeout(properties.getReadReplica().getValidationTimeout());

        config.setAutoCommit(properties.getReadReplica().isAutoCommit());
        config.setReadOnly(true);

        if (properties.getReadReplica().getLeakDetectionThreshold() > 0) {
            config.setLeakDetectionThreshold(properties.getReadReplica().getLeakDetectionThreshold());
        }

        config.setPoolName("read-replica-pool");

        log.info("Configured read replica datasource: url={}, maxPoolSize={}, minIdle={}",
            config.getJdbcUrl(), config.getMaximumPoolSize(), config.getMinimumIdle());

        return new HikariDataSource(config);
    }
}
