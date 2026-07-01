package id.payu.compliance.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("SecurityConfig should have @EnableWebSecurity annotation")
    void securityConfigShouldHaveEnableWebSecurity() {
        boolean hasAnnotation = SecurityConfig.class.isAnnotationPresent(EnableWebSecurity.class);
        assertThat(hasAnnotation)
                .as("SecurityConfig must be annotated with @EnableWebSecurity")
                .isTrue();
    }

    @Test
    @DisplayName("SecurityConfig should have @EnableMethodSecurity annotation")
    void securityConfigShouldHaveEnableMethodSecurity() {
        boolean hasAnnotation = SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class);
        assertThat(hasAnnotation)
                .as("SecurityConfig must be annotated with @EnableMethodSecurity for role-based access")
                .isTrue();
    }

    @Test
    @DisplayName("SecurityConfig should declare filterChain bean and NOT declare webSecurityCustomizer bypass")
    void securityConfigShouldDeclareBeanMethods() throws NoSuchMethodException {
        // Verify filterChain bean method exists
        assertThat(SecurityConfig.class.getDeclaredMethod("filterChain",
                org.springframework.security.config.annotation.web.builders.HttpSecurity.class))
                .as("SecurityConfig must declare a filterChain(HttpSecurity) method")
                .isNotNull();

        // Verify webSecurityCustomizer bypass was removed (AUDIT-066: actuator hardening)
        boolean hasWebSecurityCustomizer;
        try {
            SecurityConfig.class.getDeclaredMethod("webSecurityCustomizer");
            hasWebSecurityCustomizer = true;
        } catch (NoSuchMethodException e) {
            hasWebSecurityCustomizer = false;
        }
        assertThat(hasWebSecurityCustomizer)
                .as("SecurityConfig must NOT declare webSecurityCustomizer() — actuator bypass removed per AUDIT-066")
                .isFalse();
    }
}
