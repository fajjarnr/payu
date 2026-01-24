package id.payu.account.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration for Primary and Read Replica DataSources with optimized HikariCP.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Primary datasource for write operations</li>
 *   <li>Read replica datasource for reporting/analytics queries</li>
 *   <li>Tuned HikariCP connection pool settings</li>
 *   <li>Connection validation and leak detection</li>
 * </ul>
 */
@Slf4j
@Configuration
public class DataSourceConfiguration {

    /**
     * Primary datasource for write operations.
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.primary.hikari")
    public DataSource primaryDataSource() {
        log.info("Configuring primary datasource for write operations");
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * Read replica datasource for read operations.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.read-replica", name = "enabled", havingValue = "true")
    @ConfigurationProperties(prefix = "spring.datasource.read-replica.hikari")
    public DataSource readReplicaDataSource() {
        log.info("Configuring read replica datasource for read operations");
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * JdbcTemplate for read operations using read replica.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.read-replica", name = "enabled", havingValue = "true")
    public JdbcTemplate readJdbcTemplate(@Qualifier("readReplicaDataSource") DataSource readReplicaDataSource) {
        return new JdbcTemplate(readReplicaDataSource);
    }
}
