package id.payu.partner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for Partner Service.
 *
 * <p>This service manages partner integrations, including:
 * <ul>
 *   <li>Partner registration and credential management</li>
 *   <li>Certificate management and rotation</li>
 *   <li>Snap BI integration for payment processing</li>
 *   <li>Webhook handling for payment notifications</li>
 *   <li>Outbound webhook dispatch and delivery</li>
 * </ul>
 *
 * @author PayU Backend Team
 */
@EnableJpaRepositories(basePackages = "id.payu.partner.adapter.persistence.repository")
@EntityScan(basePackages = "id.payu.partner.domain")
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PartnerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PartnerServiceApplication.class, args);
    }
}
