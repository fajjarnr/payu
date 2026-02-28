package id.payu.dispute.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for Dispute Service.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "id.payu.dispute.adapter.persistence")
public class JpaConfig {
}
