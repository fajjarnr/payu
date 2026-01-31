package id.payu.partner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Partner Service.
 *
 * <p>This service manages partner integrations, including:
 * <ul>
 *   <li>Partner registration and credential management</li>
 *   <li>Certificate management and rotation</li>
 *   <li>Snap BI integration for payment processing</li>
 *   <li>Webhook handling for payment notifications</li>
 * </ul>
 *
 * @author PayU Backend Team
 */
@SpringBootApplication
public class PartnerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PartnerServiceApplication.class, args);
    }
}
