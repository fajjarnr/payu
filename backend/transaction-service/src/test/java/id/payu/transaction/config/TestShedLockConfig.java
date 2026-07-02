package id.payu.transaction.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import java.util.Optional;

@Configuration
@Profile("test")
public class TestShedLockConfig {

    @Bean
    public LockProvider lockProvider() {
        return new LockProvider() {
            @Override
            public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
                return Optional.of(new SimpleLock() {
                    @Override
                    public void unlock() {
                        // No-op
                    }
                });
            }
        };
    }
}
