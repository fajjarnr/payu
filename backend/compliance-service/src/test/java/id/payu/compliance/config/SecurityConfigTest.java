package id.payu.compliance.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("SecurityConfig should be instantiable")
    void securityConfigShouldBeInstantiable() {
        SecurityConfig securityConfig = new SecurityConfig();
        assertThat(securityConfig).isNotNull();
    }
}
