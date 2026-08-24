package id.payu.auth.config;

import id.payu.api.common.security.SecurityHeadersFilter;
import id.payu.auth.adapter.security.DPoPBearerTokenResolver;
import id.payu.auth.adapter.security.DPoPFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Security configuration for Auth Service — ADR-0062 DPoP + strict refresh rotation.
 * Uses 3 chains: Order 1 public (callback/register/refresh/mfa/device), Order 2 actuator, Order 3 JWT.
 * DPoP proof validated via DPoPFilter for bound tokens and Authorization: DPoP.
 * Fallback Bearer+mTLS for confidential clients (payu-backend/gateway).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(DPoPProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/payu}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8080/realms/payu/protocol/openid-connect/certs}")
    private String jwkSetUri;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/callback",
        "/api/v1/auth/register",
        "/api/v1/auth/logout",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password",
        "/api/v1/auth/mfa/verify",
        "/api/v1/auth/mfa/challenge",
        "/api/v1/auth/device",
        "/api/v1/auth/device/**",
        "/error",
        "/api-docs/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
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

    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http,
                                                         @Autowired(required = false) DPoPFilter dpopFilter) throws Exception {
        http
            .securityMatcher(PUBLIC_ENDPOINTS)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.disable())
            .addFilterBefore(securityHeadersFilter(), SecurityContextHolderFilter.class);
        if (dpopFilter != null) {
            http.addFilterBefore(dpopFilter, SecurityContextHolderFilter.class);
        }
        return http.build();
    }

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

    @Bean
    @Order(3)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http,
                                                      @Autowired(required = false) DPoPFilter dpopFilter,
                                                      @Autowired(required = false) DPoPBearerTokenResolver dpopResolver) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/validate").authenticated()
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> {
                if (dpopResolver != null) {
                    oauth2.bearerTokenResolver(dpopResolver);
                }
                oauth2.jwt(jwt -> {});
            })
            .addFilterBefore(securityHeadersFilter(), SecurityContextHolderFilter.class);
        if (dpopFilter != null) {
            http.addFilterAfter(dpopFilter, SecurityContextHolderFilter.class);
        }
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JwtDecoder with issuer: {}", issuerUri);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return jwtDecoder;
    }

    @Bean
    public Argon2PasswordEncoder argon2PasswordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 1 << 12, 3);
    }
}
