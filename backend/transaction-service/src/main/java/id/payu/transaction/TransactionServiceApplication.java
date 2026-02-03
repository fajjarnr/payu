package id.payu.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableFeignClients(basePackages = "id.payu.transaction.adapter.client")
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "id.payu.transaction")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "id.payu.transaction")
@EnableScheduling
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
