package id.payu.transaction.config;

import id.payu.api.common.security.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
// BUG-BE-170: Required for @PreAuthorize to be enforced
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    /**
     * Security filter chain for transaction-service.
     *
     * <p>The {@code jwtAuthenticationConverter} is auto-configured by
     * {@code security-starter} via {@code KeycloakJwtAutoConfiguration}.</p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/transaction-service/actuator/**", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/**/public/**", "/api/v1/**/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                            .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )
                .addFilterBefore(securityHeadersFilter(), SecurityContextHolderFilter.class);
        return http.build();
    }
}
