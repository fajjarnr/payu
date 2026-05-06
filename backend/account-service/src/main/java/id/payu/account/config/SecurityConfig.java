package id.payu.account.config;

import id.payu.security.converter.EncryptedStringConverter;
import id.payu.security.crypto.EncryptionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                // BUG-AUTH-028: Restrict actuator — only health/info are public
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/account-service/actuator/health", "/account-service/actuator/health/**", "/account-service/actuator/info").permitAll()
                .requestMatchers("/actuator/**", "/account-service/actuator/**").authenticated()
                // Swagger/OpenAPI docs
                .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
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
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(keycloakGrantedAuthoritiesConverter());
        return jwtAuthenticationConverter;
    }

    /**
     * Maps Keycloak JWT claims to Spring Security GrantedAuthority instances.
     * See wallet-service SecurityConfig for full documentation.
     */
    private Converter<Jwt, Collection<GrantedAuthority>> keycloakGrantedAuthoritiesConverter() {
        return jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();
            List<String> realmRoles = extractRealmRoles(jwt);

            // 1. Map realm_access.roles → ROLE_*
            for (String role : realmRoles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            // 2. Map resource_access.*.roles → ROLE_*
            extractResourceRoles(jwt).forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role))
            );

            // 3. Map scope → SCOPE_*
            String scope = jwt.getClaimAsString("scope");
            if (scope != null) {
                for (String s : scope.split(" ")) {
                    if (!s.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                    }
                }
            }

            // 4. Derive fine-grained permissions from coarse Keycloak roles
            boolean isUser = realmRoles.contains("default-roles-payu")
                    || realmRoles.stream().anyMatch(r -> r.equalsIgnoreCase("user"));
            boolean isAdmin = realmRoles.contains("admin");

            if (isUser) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                authorities.add(new SimpleGrantedAuthority("read:wallet"));
                authorities.add(new SimpleGrantedAuthority("write:wallet"));
                authorities.add(new SimpleGrantedAuthority("read:transaction"));
                authorities.add(new SimpleGrantedAuthority("write:transaction"));
                authorities.add(new SimpleGrantedAuthority("write:payment"));
                authorities.add(new SimpleGrantedAuthority("read:account"));
                authorities.add(new SimpleGrantedAuthority("write:account"));
            }

            if (isAdmin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                authorities.add(new SimpleGrantedAuthority("read:wallet"));
                authorities.add(new SimpleGrantedAuthority("write:wallet"));
                authorities.add(new SimpleGrantedAuthority("read:transaction"));
                authorities.add(new SimpleGrantedAuthority("write:transaction"));
                authorities.add(new SimpleGrantedAuthority("write:payment"));
                authorities.add(new SimpleGrantedAuthority("read:account"));
                authorities.add(new SimpleGrantedAuthority("write:account"));
            }

            return authorities;
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null ? roles : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractResourceRoles(Jwt jwt) {
        List<String> allRoles = new ArrayList<>();
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            for (Object value : resourceAccess.values()) {
                if (value instanceof Map) {
                    Map<String, Object> resource = (Map<String, Object>) value;
                    Object roles = resource.get("roles");
                    if (roles instanceof List) {
                        allRoles.addAll((List<String>) roles);
                    }
                }
            }
        }
        return allRoles;
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
