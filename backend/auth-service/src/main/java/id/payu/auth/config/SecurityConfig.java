package id.payu.auth.config;

import id.payu.api.common.security.SecurityHeadersFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Security configuration for Auth Service.
 *
 * Configures which endpoints require authentication and which are public.
 * The login endpoint must be accessible without authentication.
 *
 * Uses multiple security filter chains to prevent JWT authentication filter
 * from processing public endpoints before permitAll() can take effect.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // BUG-BE-167: Use @Value instead of System.getenv() to allow overrides in test profiles
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/payu}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8080/realms/payu/protocol/openid-connect/certs}")
    private String jwkSetUri;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password",
        // BUG-BE-166: MFA endpoints must be public — user doesn't have JWT yet during MFA flow
        "/api/v1/auth/mfa/verify",
        "/api/v1/auth/mfa/challenge"
    };

    private static final String[] PUBLIC_ACTUATOR_ENDPOINTS = {
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info"
    };

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    /**
     * Public security filter chain - handles requests without JWT authentication.
     * This chain processes public endpoints first (Order 1) to prevent JWT validation
     * from rejecting requests before permitAll() can take effect.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(PUBLIC_ENDPOINTS)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.disable())
            // BUG-BE-171: Use SecurityContextHolderFilter instead of deprecated SecurityContextPersistenceFilter
            .addFilterBefore(securityHeadersFilter(), SecurityContextHolderFilter.class);

        return http.build();
    }

    /**
     * Public actuator security filter chain - handles actuator health/info endpoints.
     * Separate chain (Order 2) for monitoring without authentication.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain publicActuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(PUBLIC_ACTUATOR_ENDPOINTS)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * JWT security filter chain - handles authenticated requests.
     * This chain (Order 3) processes all other requests and requires valid JWT.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Protected endpoints - require authentication
                .requestMatchers("/api/v1/auth/validate").authenticated()
                // Actuator endpoints - require authentication
                .requestMatchers("/actuator/**").authenticated()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            )
            // BUG-BE-171: Use SecurityContextHolderFilter instead of deprecated SecurityContextPersistenceFilter
            .addFilterBefore(securityHeadersFilter(), SecurityContextHolderFilter.class);

        return http.build();
    }

    /**
     * Configure JWT decoder for OAuth2 Resource Server.
     * This bean is required since we excluded OAuth2ResourceServerAutoConfiguration.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JwtDecoder with issuer: {}", issuerUri);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));

        return jwtDecoder;
    }
}
