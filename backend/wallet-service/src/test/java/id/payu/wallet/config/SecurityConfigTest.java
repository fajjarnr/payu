package id.payu.wallet.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("SecurityConfig should be instantiable and produce securityHeadersFilter bean")
    void securityConfigShouldBeInstantiable() {
        SecurityConfig securityConfig = new SecurityConfig();
        assertThat(securityConfig).isNotNull();

        // Verify securityHeadersFilter() bean method returns a non-null filter
        assertThat(securityConfig.securityHeadersFilter())
            .as("securityHeadersFilter bean should not be null")
            .isNotNull();

        // Verify webSecurityCustomizer() bean method returns a non-null customizer
        assertThat(securityConfig.webSecurityCustomizer())
            .as("webSecurityCustomizer bean should not be null")
            .isNotNull();
    }

    @Test
    @DisplayName("SecurityConfig should have required Spring Security annotations")
    void securityConfigShouldHaveRequiredAnnotations() {
        assertThat(SecurityConfig.class.isAnnotationPresent(Configuration.class))
            .as("SecurityConfig must be annotated with @Configuration")
            .isTrue();
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableWebSecurity.class))
            .as("SecurityConfig must be annotated with @EnableWebSecurity")
            .isTrue();
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class))
            .as("SecurityConfig must be annotated with @EnableMethodSecurity")
            .isTrue();
    }
}
