package id.payu.support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Support Service.
 *
 * <p>Disabled in test profile to avoid Spring Security 7 UnreachableFilterChainException
 * when {@code TestSecurityConfig.testSecurityFilterChain} is also active. Tests should
 * import {@code TestSecurityConfig} explicitly via {@code @Import(TestSecurityConfig.class)}.
 */
@Configuration
@Profile("!test")
@EnableWebSecurity
// BUG-BE-170: Required for @PreAuthorize to be enforced
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/support-service/actuator/health", "/support-service/actuator/health/**", "/support-service/actuator/info").permitAll()
                .requestMatchers("/actuator/**", "/support-service/actuator/**").authenticated()
                .requestMatchers("/v1/public/**", "/api/v1/v1/public/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }
}
