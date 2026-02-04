package id.payu.transaction.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration for Transaction Service.
 * Separated from the main application class to allow for easier slice testing.
 */
@Configuration
@EntityScan(basePackages = "id.payu.transaction")
@EnableJpaRepositories(basePackages = "id.payu.transaction")
public class JpaConfig {
}
