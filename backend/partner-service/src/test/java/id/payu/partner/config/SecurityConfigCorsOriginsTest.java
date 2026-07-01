package id.payu.partner.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUDIT-053 fix: partner-service {@link SecurityConfig#corsConfigurationSource()}
 * must source allowed origins from a Spring {@code @Value}-injected property
 * instead of {@code System.getenv()}.
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
    void valueDefaultExpressionShouldPreservePartnerProductionOrigins() throws Exception {
        // partner-service uses payu.co.id production origins as default
        // (not localhost — partner domain has stricter prod defaults).
        Field field = SecurityConfig.class.getDeclaredField("allowedOrigins");
        Value annotation = field.getAnnotation(Value.class);

        assertThat(annotation.value())
            .contains("https://payu.co.id")
            .contains("https://partner.payu.co.id");
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
