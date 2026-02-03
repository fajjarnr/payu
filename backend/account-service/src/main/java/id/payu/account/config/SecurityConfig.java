package id.payu.account.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Public endpoints that bypass JWT authentication entirely.
     * This filter chain is evaluated first (Order 1) and handles requests without OAuth2 Resource Server.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/accounts/register", "/api/v1/auth/login")
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // Explicitly disable OAuth2 Resource Server for public endpoints
            .oauth2ResourceServer(oauth2 -> oauth2.disable());

        log.info("Public security filter chain configured for: /api/v1/accounts/register, /api/v1/auth/login");
        return http.build();
    }

    /**
     * JWT-authenticated endpoints for all other requests.
     * This filter chain is evaluated second (Order 2) and requires valid JWT tokens.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        log.info("JWT security filter chain configured for authenticated endpoints");
        return http.build();
    }

    /**
     * Bypass Spring Security entirely for actuator endpoints.
     * This is applied AFTER the filter chains above, as a final fallback.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/actuator/**", "/account-service/actuator/**");
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    /**
     * Configure JWT decoder for OAuth2 Resource Server.
     * This bean is required since we excluded OAuth2ResourceServerAutoConfiguration.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String issuerUri = System.getenv().getOrDefault("OIDC_ISSUER", "http://localhost:8080/realms/payu");
        String jwkSetUri = System.getenv().getOrDefault("OIDC_JWK_SET_URI",
            "http://localhost:8080/realms/payu/protocol/openid-connect/certs");

        log.info("Configuring JwtDecoder with issuer: {}", issuerUri);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));

        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // SECURITY: Restrict CORS to specific origins only
        // Use environment variable to configure allowed origins for different environments
        String allowedOrigins = System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:8080");

        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Request-ID", "X-Correlation-ID", "X-Device-ID"));
        configuration.setExposedHeaders(Arrays.asList("X-Request-ID", "X-RateLimit-Remaining"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
