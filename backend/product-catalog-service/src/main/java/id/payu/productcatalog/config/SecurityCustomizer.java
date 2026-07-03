package id.payu.productcatalog.config;

import id.payu.security.config.SecurityConfigurerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityCustomizer {

    @Bean
    public SecurityConfigurerCustomizer productCatalogSecurityCustomizer() {
        return auth -> auth
                .requestMatchers("/products/**").permitAll()
                .requestMatchers("/admin/**").authenticated();
    }
}
