package id.payu.wallet.config;

import id.payu.api.common.security.SecurityHeadersFilter;
import id.payu.security.config.SecurityConfigurerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
public class SecurityCustomizer {

    @Bean
    SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Bean
    public SecurityConfigurerCustomizer walletSecurityCustomizer(SecurityHeadersFilter filter) {
        return new SecurityConfigurerCustomizer() {
            @Override
            public void customize(
                    org.springframework.security.config.annotation.web.configurers
                            .AuthorizeHttpRequestsConfigurer<
                            org.springframework.security.config.annotation.web.builders.HttpSecurity>
                                    .AuthorizationManagerRequestMatcherRegistry auth) {
                // wallet-service has no extra public endpoints beyond the shared defaults
            }

            @Override
            public void configure(
                    org.springframework.security.config.annotation.web.builders.HttpSecurity http)
                    throws Exception {
                http.addFilterBefore(filter, SecurityContextHolderFilter.class);
            }
        };
    }
}
