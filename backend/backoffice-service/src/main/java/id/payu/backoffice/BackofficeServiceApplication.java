package id.payu.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "id.payu")
@EnableAsync
@ConfigurationPropertiesScan
public class BackofficeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackofficeServiceApplication.class, args);
    }
}
