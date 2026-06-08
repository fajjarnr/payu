package id.payu.promotion;

import id.payu.saga.annotation.EnableSaga;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application class for PayU PromotionEntity Service.
 *
 * This service manages:
 * - Promotions and campaigns
 * - CashbackEntity rewards
 * - Loyalty points
 * - Referrals
 * - Customer segmentation
 */
@EnableJpaRepositories(basePackages = "id.payu.promotion.adapter.persistence.repository")
@EntityScan(basePackages = {"id.payu.promotion.domain", "id.payu.promotion.adapter.persistence.entity", "id.payu.saga.entity"})
@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
@EnableAsync
@EnableSaga
public class PromotionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionServiceApplication.class, args);
    }
}
