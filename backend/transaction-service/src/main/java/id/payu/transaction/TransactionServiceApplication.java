package id.payu.transaction;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "id.payu.transaction.adapter.persistence.repository")
@EntityScan(basePackages = {"id.payu.transaction.domain.model", "id.payu.transaction.adapter.persistence.entity"})
@SpringBootApplication
@EnableKafka
@EnableFeignClients(basePackages = "id.payu.transaction.adapter.client")
@EnableAsync
@EnableScheduling
// ITER-53: ShedLock distributed locking for @Scheduled methods.
// Default lockAtMostFor covers any single method execution; lockAtLeastFor
// prevents clock-skew issues where another replica could immediately re-acquire.
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M", defaultLockAtLeastFor = "PT1S")
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
