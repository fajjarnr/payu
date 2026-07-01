package id.payu.cms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Security configuration for CMS Service.
 * Configures OAuth2/Keycloak JWT authentication.
 *
 * <p>The {@code jwtAuthenticationConverter} is auto-configured by
 * {@code security-starter} via {@code KeycloakJwtAutoConfiguration}.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String SWAGGER_PATH = "/swagger-ui/**";
    private static final String API_DOCS_PATH = "/v3/api-docs/**";
    private static final String PUBLIC_API_PATH = "/api/v1/public/**";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/v1/public/**", "/api/v1/v1/public/**").permitAll()
                .requestMatchers(PUBLIC_API_PATH).permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/cms-service/actuator/health", "/cms-service/actuator/health/**", "/cms-service/actuator/info").permitAll()
                .requestMatchers("/actuator/**", "/cms-service/actuator/**").authenticated()
                .requestMatchers(SWAGGER_PATH).permitAll()
                .requestMatchers(API_DOCS_PATH).permitAll()
                // Admin endpoints - require authentication
                .requestMatchers("/api/v1/contents/**").authenticated()
                // Health check
                .requestMatchers("/health", "/readiness", "/liveness").permitAll()
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            );

        return http.build();
    }


}
