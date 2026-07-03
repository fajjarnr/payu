package id.payu.compliance.config;

import id.payu.security.config.SecurityConfigurerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityCustomizer {

    @Bean
    public SecurityConfigurerCustomizer complianceSecurityCustomizer() {
        return auth -> auth
                .requestMatchers("/api/v1/compliance/**").hasAnyRole("COMPLIANCE_OFFICER", "ADMIN");
    }
}
