package id.payu.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityProductionDefaultsTest {

    @Test
    void productionRejectsMissingEncryptionSalt() {
        SecurityProperties properties = new SecurityProperties();
        properties.setEncryptionEnabled(true);
        properties.getEncryption().setPassword("production-encryption-key");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("container");

        assertThatThrownBy(() -> new SecurityAutoConfiguration(
                properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("salt");
    }

    @Test
    void productionRejectsMissingEncryptionPassword() {
        SecurityProperties properties = new SecurityProperties();
        properties.setEncryptionEnabled(true);
        properties.getEncryption().setSalt("production-pbkdf2-salt");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("container");

        assertThatThrownBy(() -> new SecurityAutoConfiguration(
                properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("password");
    }

    @Test
    void productionRejectsDisabledPiiProtections() {
        SecurityProperties properties = new SecurityProperties();
        properties.setMaskingEnabled(false);
        properties.setAuditEnabled(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new SecurityAutoConfiguration(
                properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("masking");
    }
}
