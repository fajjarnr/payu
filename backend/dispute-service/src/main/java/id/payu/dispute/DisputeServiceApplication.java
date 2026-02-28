package id.payu.dispute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Dispute Service.
 *
 * <p>This service handles refund and dispute management for partner transactions.
 * It implements hexagonal architecture with clear separation between domain,
 * application, and adapter layers.</p>
 */
@SpringBootApplication
public class DisputeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisputeServiceApplication.class, args);
    }
}
