package id.payu.transaction.config;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.context.annotation.Profile;
import java.util.TimeZone;

/**
 * ITER-53: ShedLock configuration. Uses JdbcTemplate-based LockProvider that
 * stores locks in the {@code shedlock} table (created by V21__add_shedlock_table.sql).
 * <p>
 * On multi-replica deployment, only one replica can hold a lock at a time.
 * Other replicas skip the @Scheduled method and wait for the next tick.
 * <p>
 * TXN-HARDEN-004/005: usingDbTime() avoids clock drift across pods; forceUtcTimeZone via withTimeZone(UTC)
 * per ADR-0042 Context7 /lukas-krecan/shedlock.
 */
@Configuration
@Profile("!test")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTimeZone(TimeZone.getTimeZone("UTC"))
                        .usingDbTime()
                        .build()
        );
    }
}
