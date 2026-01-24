package id.payu.abtesting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for PayU A/B Testing Service
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class AbTestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbTestingApplication.class, args);
    }
}
