package id.payu.account.config;

import id.payu.security.converter.EncryptedStringConverter;
import id.payu.security.crypto.EncryptionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${payu.security.encryption-enabled:false}")
    private boolean encryptionEnabled;

    @Value("${payu.security.encryption.password:}")
    private String encryptionPassword;

    @Value("${payu.security.encryption.salt:}")
    private String encryptionSalt;

    /**
     * Initialize EncryptedStringConverter directly to work around SecurityAutoConfiguration
     * not loading in certain container environments.
     * This ensures JPA @Convert annotations on User entity fields (email, phoneNumber) work correctly.
     */
    @PostConstruct
    public void initEncryptedStringConverter() {
        if (encryptionEnabled && encryptionPassword != null && !encryptionPassword.isEmpty()) {
            try {
                EncryptionService encryptionService = new EncryptionService(
                        encryptionPassword,
                        Collections.emptyList(),
                        encryptionSalt != null && !encryptionSalt.isEmpty() ? encryptionSalt : null
                );
                EncryptedStringConverter.setEncryptionService(encryptionService);
                log.info("EncryptedStringConverter initialized with EncryptionService (encryption enabled)");
            } catch (Exception e) {
                log.warn("Failed to initialize EncryptionService, falling back to pass-through mode: {}", e.getMessage());
                EncryptedStringConverter.setEncryptionDisabled(true);
            }
        } else {
            EncryptedStringConverter.setEncryptionDisabled(true);
            log.info("EncryptedStringConverter set to pass-through mode (encryption-enabled={})", encryptionEnabled);
        }
    }

    /**
     * Single security filter chain with proper authorization rules.
     * Public endpoints (registration, actuator, swagger) are permitted without authentication.
     * All other endpoints require a valid JWT token from Keycloak.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/api/v1/accounts/register").permitAll()
                // Actuator endpoints
                .requestMatchers("/actuator/**", "/account-service/actuator/**").permitAll()
                // Swagger/OpenAPI docs
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        log.info("Security filter chain configured: /api/v1/accounts/register=public, all others=JWT authenticated");
        return http.build();
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
