package id.payu.lending;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "id.payu.lending.repository")
@EntityScan(basePackages = "id.payu.lending.entity")
@SpringBootApplication
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M", defaultLockAtLeastFor = "PT1S")
@org.springframework.cloud.openfeign.EnableFeignClients(basePackages = "id.payu.lending")
public class LendingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LendingServiceApplication.class, args);
    }
}
