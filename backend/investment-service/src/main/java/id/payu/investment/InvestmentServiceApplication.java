package id.payu.investment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "id.payu.investment.adapter.persistence.repository")
@org.springframework.boot.persistence.autoconfigure.EntityScan(basePackages = "id.payu.investment.adapter.persistence")
@SpringBootApplication
@EnableAsync
public class InvestmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestmentServiceApplication.class, args);
    }
}
