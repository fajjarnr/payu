package id.payu.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Billing Service Application - Spring Boot main class.
 * Handles bill payments for PLN, PDAM, Pulsa, and E-wallet top-ups.
 */
@EnableJpaRepositories(basePackages = "id.payu.billing.adapter.persistence.repository")
@EntityScan(basePackages = "id.payu.billing.domain.model")
@SpringBootApplication
@EnableTransactionManagement
@EnableKafka
@EnableCaching
@EnableAsync
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
