package id.payu.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = {"id.payu.account.adapter.persistence.repository", "id.payu.account.repository"})
@EntityScan(basePackages = {"id.payu.account.adapter.persistence.entity", "id.payu.account.entity"})
@SpringBootApplication(exclude = {OAuth2ResourceServerAutoConfiguration.class})
@EnableFeignClients
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

}
