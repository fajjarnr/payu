package id.payu.account.config;

import id.payu.security.config.SecurityConfigurerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityCustomizer {

    @Bean
    public SecurityConfigurerCustomizer accountSecurityCustomizer() {
        return auth -> auth
                .requestMatchers("/api/v1/accounts/register").permitAll();
    }
}
