package id.payu.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * PayU Support Service Application
 *
 * Support team training management service for PayU Digital Banking Platform.
 * This service manages support agents, training modules, and training progress tracking.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "id.payu.support.repository")
public class SupportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportServiceApplication.class, args);
    }
}
