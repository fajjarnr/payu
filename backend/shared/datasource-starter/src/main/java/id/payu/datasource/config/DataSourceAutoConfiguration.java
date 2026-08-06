package id.payu.datasource.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Shared DataSource auto-configuration with HikariCP.
 * <p>
 * Provides primary (write) and read-replica (read) DataSources
 * configured via standard {@code spring.datasource.*.hikari} properties.
 * </p>
 * <p>
 * Disabled in {@code container} profile (Testcontainers/CI) — Spring Boot
 * auto-configuration handles test DataSources instead.
 * </p>
 */
@AutoConfiguration
@Profile("!container")
@ConditionalOnClass(HikariDataSource.class)
public class DataSourceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataSourceAutoConfiguration.class);

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "primaryDataSource")
    @ConditionalOnProperty(prefix = "spring.datasource.primary.hikari", name = "jdbc-url")
    @ConfigurationProperties(prefix = "spring.datasource.primary.hikari")
    public HikariDataSource primaryDataSource() {
        log.info("Configuring primary datasource for write operations");
        return new HikariDataSource();
    }

    @Bean
    @ConditionalOnMissingBean(name = "readReplicaDataSource")
    @ConditionalOnProperty(prefix = "spring.datasource.read-replica", name = "enabled", havingValue = "true")
    @ConfigurationProperties(prefix = "spring.datasource.read-replica.hikari")
    public HikariDataSource readReplicaDataSource() {
        log.info("Configuring read replica datasource for read operations");
        return new HikariDataSource();
    }

    @Bean
    @ConditionalOnMissingBean(name = "readJdbcTemplate")
    @ConditionalOnProperty(prefix = "spring.datasource.read-replica", name = "enabled", havingValue = "true")
    public JdbcTemplate readJdbcTemplate(@Qualifier("readReplicaDataSource") DataSource readReplicaDataSource) {
        return new JdbcTemplate(readReplicaDataSource);
    }
}
