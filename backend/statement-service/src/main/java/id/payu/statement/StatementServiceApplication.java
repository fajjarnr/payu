package id.payu.statement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * PayU StatementEntity Service Application
 *
 * Service for generating and managing monthly e-statements (PDF) for users.
 * Supports transaction history aggregation, balance snapshots, and secure PDF delivery.
 */
@EnableJpaRepositories(basePackages = "id.payu.statement.adapter.persistence.repository")
@org.springframework.boot.persistence.autoconfigure.EntityScan(basePackages = "id.payu.statement.adapter.persistence.entity")
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableKafka
public class StatementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatementServiceApplication.class, args);
    }
}
