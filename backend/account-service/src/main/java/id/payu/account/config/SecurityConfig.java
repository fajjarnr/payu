package id.payu.account.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@Profile("!test")
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * CORS allowed origins for the account-service API.
     *
     * <p>Driven by Spring {@code @Value} (AUDIT-053 fix). Default preserves
     * localhost dev fallback.</p>
     */
    @Value("${payu.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    /**
     * OIDC issuer URI used by the JWT decoder to validate token signatures.
     *
     * <p>Driven by Spring {@code @Value} (AUDIT-053 fix).</p>
     */
    @Value("${payu.security.oauth2.issuer-uri:http://localhost:8080/realms/payu}")
    private String oidcIssuerUri;

    /**
     * OIDC JWK set URI used by the JWT decoder to fetch public keys.
     *
     * <p>Driven by Spring {@code @Value} (AUDIT-053 fix).</p>
     */
    @Value("${payu.security.oauth2.jwk-set-uri:http://localhost:8080/realms/payu/protocol/openid-connect/certs}")
    private String oidcJwkSetUri;



    /**
     * Single security filter chain with proper authorization rules.
     * Public endpoints (registration, actuator, swagger) are permitted without authentication.
     * All other endpoints require a valid JWT token from Keycloak.
     *
     * <p>The {@code jwtAuthenticationConverter} is auto-configured by
     * {@code security-starter} via {@code KeycloakJwtAutoConfiguration}.</p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/api/v1/accounts/register").permitAll()
                // BUG-AUTH-028: Restrict actuator — only health/info are public
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/account-service/actuator/health", "/account-service/actuator/health/**", "/account-service/actuator/info").permitAll()
                .requestMatchers("/actuator/**", "/account-service/actuator/**").authenticated()
                // Swagger/OpenAPI docs (collapsed to 3 /** catch-alls, no typo)
                .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            );

        log.info("Security filter chain configured: /api/v1/accounts/register=public, all others=JWT authenticated");
        return http.build();
    }

    /**
     * Configure JWT decoder for OAuth2 Resource Server.
     * This bean is required since we excluded OAuth2ResourceServerAutoConfiguration.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JwtDecoder with issuer: {}", oidcIssuerUri);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(oidcJwkSetUri).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(oidcIssuerUri));

        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // SECURITY: Restrict CORS to specific origins only
        // Use @Value-injected property to configure allowed origins per environment
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
