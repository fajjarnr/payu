package id.payu.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * PayU CMS Service Application
 *
 * Content Management Service for banners, promotions, alerts, and popups.
 * Supports scheduled publishing, targeting rules, and A/B test integration.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
public class CmsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmsServiceApplication.class, args);
    }
}
