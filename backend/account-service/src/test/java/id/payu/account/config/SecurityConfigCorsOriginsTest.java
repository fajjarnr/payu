package id.payu.account.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUDIT-053 fix: account-service {@link SecurityConfig} must source all
 * externalized configuration (CORS origins, OIDC issuer, OIDC JWK set URI)
 * from Spring {@code @Value}-injected properties instead of
 * {@code System.getenv()} calls.
 *
 * <p>The {@link SecurityConfig} class is annotated with
 * {@code @Profile("!test")} so it is not loaded in unit tests by default,
 * but the reflection-based field assertions below work regardless of profile
 * because they inspect the class structure directly.</p>
 */
class SecurityConfigCorsOriginsTest {

    @Test
    void shouldHaveValueAnnotationOnAllowedOriginsField() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("allowedOrigins");

        Value annotation = field.getAnnotation(Value.class);
        assertThat(annotation)
            .as("@Value annotation must drive allowedOrigins (not System.getenv)")
            .isNotNull();
        assertThat(annotation.value())
            .contains("payu.security.cors.allowed-origins");
    }

    @Test
    void shouldHaveValueAnnotationOnOidcIssuerUriField() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("oidcIssuerUri");

        Value annotation = field.getAnnotation(Value.class);
        assertThat(annotation)
            .as("@Value annotation must drive oidcIssuerUri (not System.getenv)")
            .isNotNull();
        assertThat(annotation.value())
            .contains("payu.security.oauth2.issuer-uri");
    }

    @Test
    void shouldHaveValueAnnotationOnOidcJwkSetUriField() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("oidcJwkSetUri");

        Value annotation = field.getAnnotation(Value.class);
        assertThat(annotation)
            .as("@Value annotation must drive oidcJwkSetUri (not System.getenv)")
            .isNotNull();
        assertThat(annotation.value())
            .contains("payu.security.oauth2.jwk-set-uri");
    }

    @Test
    void valueDefaultExpressionsShouldPreserveDevFallbacks() throws Exception {
        Field corsField = SecurityConfig.class.getDeclaredField("allowedOrigins");
        Field issuerField = SecurityConfig.class.getDeclaredField("oidcIssuerUri");
        Field jwksField = SecurityConfig.class.getDeclaredField("oidcJwkSetUri");

        assertThat(corsField.getAnnotation(Value.class).value())
            .contains("http://localhost:3000")
            .contains("http://localhost:8080");
        assertThat(issuerField.getAnnotation(Value.class).value())
            .contains("http://localhost:8080/realms/payu");
        assertThat(jwksField.getAnnotation(Value.class).value())
            .contains("/protocol/openid-connect/certs");
    }

    @Test
    void corsConfigurationSourceUsesInjectedOriginsField() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins",
            "https://test.payu.co.id,https://admin.payu.co.id");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/any"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns())
            .containsExactly("https://test.payu.co.id", "https://admin.payu.co.id");
    }
}
