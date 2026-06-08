package id.payu.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "id.payu.transaction.adapter.persistence.repository")
@EntityScan(basePackages = {"id.payu.transaction.domain.model", "id.payu.transaction.adapter.persistence.entity"})
@SpringBootApplication
@EnableKafka
@EnableFeignClients(basePackages = "id.payu.transaction.adapter.client")
@EnableScheduling
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
