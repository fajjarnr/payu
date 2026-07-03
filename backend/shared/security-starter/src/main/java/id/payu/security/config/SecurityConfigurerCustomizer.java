package id.payu.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Functional interface for per-service security customizations.
 * <p>
 * Declare a {@code @Bean} of this type in any service to customize the
 * shared {@code SecurityFilterChain} without writing a full SecurityConfig.
 * </p>
 *
 * <pre>{@code
 * @Bean
 * public SecurityConfigurerCustomizer myEndpoints() {
 *     return auth -> auth
 *         .requestMatchers("/api/v1/public/**").permitAll()
 *         .requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
 * }
 * }</pre>
 *
 * <p>The default {@code configure(HttpSecurity)} method is a no-op.
 * Override it to add filters, headers, or other non-auth customizations.</p>
 */
@FunctionalInterface
public interface SecurityConfigurerCustomizer {

    /**
     * Customize authorization rules. Called after the common rules
     * (actuator health, swagger) and before {@code anyRequest().authenticated()}.
     */
    void customize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth)
            throws Exception;

    /**
     * Additional HttpSecurity configuration (filters, headers, etc.).
     * Default is no-op.
     */
    default void configure(HttpSecurity http) throws Exception {
        // no-op
    }
}
