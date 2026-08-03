package id.payu.loanorigination;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"id.payu.loanorigination", "id.payu.outbox", "id.payu.shared.restclient"})
@EntityScan(basePackages = "id.payu.loanorigination.adapter.persistence")
@EnableJpaRepositories(basePackages = "id.payu.loanorigination.adapter.persistence")
public class LoanOriginationApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanOriginationApplication.class, args);
    }
}
