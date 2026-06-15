package id.payu.dispute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "id.payu.dispute.adapter.persistence.repository")
@EntityScan(basePackages = {"id.payu.dispute.domain.model", "id.payu.dispute.adapter.persistence.entity"})
@SpringBootApplication
public class DisputeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisputeServiceApplication.class, args);
    }
}
