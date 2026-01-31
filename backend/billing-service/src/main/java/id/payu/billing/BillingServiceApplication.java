package id.payu.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Billing Service Application - Spring Boot main class.
 * Handles bill payments for PLN, PDAM, Pulsa, and E-wallet top-ups.
 */
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
