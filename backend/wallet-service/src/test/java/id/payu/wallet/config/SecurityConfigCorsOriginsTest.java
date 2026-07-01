package id.payu.wallet.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUDIT-053 fix: wallet-service {@link SecurityConfig#corsConfigurationSource()}
 * must source allowed origins from a Spring {@code @Value}-injected property
 * instead of {@code System.getenv()} so the value can be overridden via
 * {@code application.yml}, {@code SPRING_APPLICATION_JSON}, or
 * {@code @TestPropertySource}.
 *
 * <p>Tests use {@link ReflectionTestUtils} to inject the field directly,
 * avoiding the cost of loading the full Spring Security context (which would
 * need {@code HttpSecurity} + {@code JwtAuthenticationConverter} mocks).</p>
 */
class SecurityConfigCorsOriginsTest {

    @Test
    void shouldHaveValueAnnotationOnAllowedOriginsField() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("allowedOrigins");

        Value annotation = field.getAnnotation(Value.class);
        assertThat(annotation)
            .as("@Value annotation must drive allowedOrigins (not System.getenv)")
            .isNotNull();
        // The @Value property follows Spring Boot naming convention
        // (payu.security.cors.allowed-origins). The env var CORS_ALLOWED_ORIGINS
        // is mapped to this property via application.yml placeholder:
        //   payu.security.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:...}
        assertThat(annotation.value())
            .as("property placeholder must follow payu.security.cors.allowed-origins convention")
            .contains("payu.security.cors.allowed-origins");
    }

    @Test
    void valueDefaultExpressionShouldPreserveLocalhostDevFallback() throws Exception {
        // The default expression in @Value must match the existing dev fallback
        // so local docker-compose keeps working without env vars.
        Field field = SecurityConfig.class.getDeclaredField("allowedOrigins");
        Value annotation = field.getAnnotation(Value.class);

        assertThat(annotation.value())
            .contains("http://localhost:3000")
            .contains("http://localhost:8080");
    }

    @Test
    void corsConfigurationSourceUsesInjectedOriginsField() {
        // Verifies the @Value field value actually flows into the CorsConfiguration.
        // Before the AUDIT-053 fix, the method called System.getenv() inline and
        // this field did not exist — ReflectionTestUtils.setField threw.
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
