package ${{ values.java_package }};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for ${{ values.service_name }}.
 */
@SpringBootApplication(scanBasePackages = {"${{ values.java_package }}", "id.payu"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
