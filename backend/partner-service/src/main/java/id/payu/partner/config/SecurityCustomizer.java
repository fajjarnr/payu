package id.payu.partner.config;

import id.payu.security.config.SecurityConfigurerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityCustomizer {

    @Bean
    public SecurityConfigurerCustomizer partnerSecurityCustomizer() {
        return auth -> auth
                .requestMatchers("/api/v1/partners/callback/**").permitAll()
                .requestMatchers("/pay/**").permitAll();
    }
}
