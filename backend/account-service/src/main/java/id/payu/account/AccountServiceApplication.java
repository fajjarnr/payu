package id.payu.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaRepositories(basePackages = "id.payu.account.adapter.persistence.repository")
@EntityScan(basePackages = "id.payu.account.adapter.persistence.entity")
@SpringBootApplication(exclude = {OAuth2ResourceServerAutoConfiguration.class})
@EnableFeignClients
@EnableAsync
@EnableScheduling
// ITER-53: ShedLock distributed locking for @Scheduled methods.
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M", defaultLockAtLeastFor = "PT1S")
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

}
