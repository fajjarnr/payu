package id.payu.promotion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application class for PayU Promotion Service.
 *
 * This service manages:
 * - Promotions and campaigns
 * - Cashback rewards
 * - Loyalty points
 * - Referrals
 * - Gamification features (badges, levels, daily check-ins)
 * - Customer segmentation
 */
@EnableJpaRepositories(basePackages = "id.payu.promotion.adapter.persistence.repository")
@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
@EnableAsync
public class PromotionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionServiceApplication.class, args);
    }
}
