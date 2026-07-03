package id.payu.fx.config;

import id.payu.security.config.SecurityConfigurerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityCustomizer {

    @Bean
    public SecurityConfigurerCustomizer fxSecurityCustomizer() {
        return auth -> auth
                .requestMatchers("/v1/rates/**").permitAll()
                .requestMatchers("/v1/conversions/estimate").permitAll();
    }
}
