package id.payu.investment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@EnableJpaRepositories(basePackages = "id.payu.investment.adapter.persistence.repository")
@org.springframework.boot.persistence.autoconfigure.EntityScan(basePackages = "id.payu.investment.adapter.persistence")
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M", defaultLockAtLeastFor = "PT1S")
public class InvestmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestmentServiceApplication.class, args);
    }
}
