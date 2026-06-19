package id.payu.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableJpaRepositories(basePackages = {"id.payu.account.adapter.persistence.repository", "id.payu.account.repository"})
@EntityScan(basePackages = {"id.payu.account.adapter.persistence.entity", "id.payu.account.entity"})
@SpringBootApplication(exclude = {OAuth2ResourceServerAutoConfiguration.class})
@EnableFeignClients
@EnableAsync
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

}
