package id.payu.productcatalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the product catalog service.
 *
 * <p>Disabled in test profile to avoid @WebMvcTest slice failures (JwtAuthenticationConverter
 * bean not auto-created in test slices). Tests use default permissive @WebMvcTest security
 * unless explicitly importing a TestSecurityConfig.</p>
 *
 * <p>The {@code jwtAuthenticationConverter} is auto-configured by
 * {@code security-starter} via {@code KeycloakJwtAutoConfiguration}.</p>
 */
@Configuration
@Profile("!test")
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Public endpoints - active products only
                    .requestMatchers("/products/**").permitAll()
                    // Health and metrics
                    .requestMatchers("/actuator/**").permitAll()
                    // API docs
                    .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v1/public/**", "/api/v1/v1/public/**").permitAll()
                    // Admin endpoints require authentication
                    .requestMatchers("/admin/**").authenticated()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }
}
