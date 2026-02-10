package id.payu.fx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "id.payu.fx.adapter.persistence.repository")
@SpringBootApplication
@EnableKafka
@EnableFeignClients
@EnableCaching
public class FxServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FxServiceApplication.class, args);
    }
}
