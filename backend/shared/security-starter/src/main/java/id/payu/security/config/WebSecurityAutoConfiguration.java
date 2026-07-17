package id.payu.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Shared web security auto-configuration.
 * <p>
 * Provides a default {@link SecurityFilterChain} with:
 * <ul>
 *   <li>CSRF disabled, stateless sessions</li>
 *   <li>OAuth2 Resource Server JWT</li>
 *   <li>Actuator health/info permitAll, rest authenticated</li>
 *   <li>Swagger/OpenAPI permitAll</li>
 *   <li>{@link SecurityConfigurerCustomizer} hooks for per-service endpoints</li>
 *   <li>CORS via {@code payu.security.cors.enabled} property</li>
 * </ul>
 * <p>
 * <b>Backs off</b> via {@code @ConditionalOnMissingBean(SecurityFilterChain.class)}
 * — services like {@code auth-service} (multi-chain) or {@code transaction-service}
 * (custom filters) define their own chain and skip this auto-config entirely.
 * </p>
 * <p>
 * Disabled in {@code test} profile. Tests that need security must explicitly
 * import a test security configuration.
 * </p>
 */
@AutoConfiguration
@Profile("!test")
@ConditionalOnWebApplication
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnMissingBean(SecurityFilterChain.class)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class WebSecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebSecurityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtAuthenticationConverter> jwtConverterProvider,
            ObjectProvider<SecurityConfigurerCustomizer> customizerProvider,
            ObjectProvider<CorsConfigurationSource> corsSourceProvider) throws Exception {

        JwtAuthenticationConverter jwtConverter = jwtConverterProvider.getIfAvailable();
        CorsConfigurationSource corsSource = corsSourceProvider.getIfAvailable();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {
                if (corsSource != null) {
                    cors.configurationSource(corsSource);
                }
            })
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // Common public endpoints (direct and per-service paths)
                auth.requestMatchers(
                        "/actuator/health", "/actuator/health/**", "/actuator/info"
                ).permitAll();

                auth.requestMatchers("/actuator/**").authenticated();

                // Swagger / OpenAPI
                auth.requestMatchers(
                    "/api-docs/**", "/v3/api-docs/**",
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v1/public/**", "/api/v1/public/**"
                ).permitAll();

                // Apply per-service customizers
                customizerProvider.forEach(customizer -> {
                    try {
                        customizer.customize(auth);
                    } catch (Exception e) {
                        throw new RuntimeException("Security configurer customizer failed", e);
                    }
                });

                // Everything else requires authentication
                auth.anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    if (jwtConverter != null) {
                        jwt.jwtAuthenticationConverter(jwtConverter);
                    }
                })
            );

        // Apply per-service HttpSecurity customizations (filters, headers, etc.)
        customizerProvider.forEach(customizer -> {
            try {
                customizer.configure(http);
            } catch (Exception e) {
                throw new RuntimeException("Security configurer configure(HttpSecurity) failed", e);
            }
        });

        log.info("Shared security filter chain configured ({} customizer(s))",
                (int) customizerProvider.stream().count());
        return http.build();
    }

    /**
     * CORS configuration — only active when {@code payu.security.cors.enabled=true}.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security.cors", name = "enabled", havingValue = "true")
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        SecurityProperties.Cors corsProps = properties.getCors();

        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = corsProps.getAllowedOrigins() != null && !corsProps.getAllowedOrigins().isEmpty()
                ? corsProps.getAllowedOrigins()
                : Collections.singletonList("http://localhost:3000");
        configuration.setAllowedOriginPatterns(origins);

        configuration.setAllowedMethods(
                corsProps.getAllowedMethods() != null
                        ? corsProps.getAllowedMethods()
                        : Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(
                corsProps.getAllowedHeaders() != null
                        ? corsProps.getAllowedHeaders()
                        : Arrays.asList("Authorization", "Content-Type", "X-Request-ID", "X-Correlation-ID"));

        if (corsProps.getExposedHeaders() != null) {
            configuration.setExposedHeaders(corsProps.getExposedHeaders());
        }

        configuration.setAllowCredentials(corsProps.isAllowCredentials());

        if (corsProps.getMaxAge() > 0) {
            configuration.setMaxAge(corsProps.getMaxAge());
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS configured: origins={}, methods={}, credentials={}",
                origins, configuration.getAllowedMethods(), configuration.getAllowCredentials());
        return source;
    }
}
