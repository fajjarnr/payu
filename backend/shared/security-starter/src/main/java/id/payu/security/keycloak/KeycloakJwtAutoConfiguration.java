package id.payu.security.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Auto-configuration that publishes a pre-configured {@link JwtAuthenticationConverter}
 * with {@link KeycloakJwtAuthoritiesConverter} for Keycloak role extraction.
 *
 * <p>This configuration is activated only when:
 * <ul>
 *   <li>{@code JwtAuthenticationConverter} is on the classpath (i.e., OAuth2 Resource Server)</li>
 *   <li>No other {@code JwtAuthenticationConverter} bean has been defined by the service</li>
 * </ul>
 *
 * <p>Services that need a custom converter can simply define their own
 * {@code JwtAuthenticationConverter} bean, and this auto-configuration will back off.</p>
 *
 * <h3>Configuration Properties</h3>
 * <ul>
 *   <li>{@code payu.security.keycloak.derive-permissions} — Enable/disable derived
 *       fine-grained permissions from coarse Keycloak roles (default: {@code true})</li>
 * </ul>
 *
 * @see KeycloakJwtAuthoritiesConverter
 */
@AutoConfiguration
@ConditionalOnClass(JwtAuthenticationConverter.class)
public class KeycloakJwtAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtAutoConfiguration.class);

    @Value("${payu.security.keycloak.derive-permissions:true}")
    private boolean derivePermissions;

    @Bean
    @ConditionalOnMissingBean
    public KeycloakJwtAuthoritiesConverter keycloakJwtAuthoritiesConverter() {
        log.info("Initializing KeycloakJwtAuthoritiesConverter (derive-permissions={})", derivePermissions);
        return new KeycloakJwtAuthoritiesConverter(derivePermissions);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            KeycloakJwtAuthoritiesConverter keycloakConverter) {
        log.info("Auto-configuring JwtAuthenticationConverter with Keycloak role extraction");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(keycloakConverter);
        return converter;
    }
}
