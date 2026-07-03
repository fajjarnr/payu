package id.payu.cms.config;

import id.payu.security.config.SecurityConfigurerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityCustomizer {

    @Bean
    public SecurityConfigurerCustomizer cmsSecurityCustomizer() {
        return auth -> auth
                .requestMatchers("/v1/public/**", "/api/v1/v1/public/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/contents/**").authenticated()
                .requestMatchers("/health", "/readiness", "/liveness").permitAll();
    }
}
