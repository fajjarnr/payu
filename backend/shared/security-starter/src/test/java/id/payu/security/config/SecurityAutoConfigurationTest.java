package id.payu.security.config;

import id.payu.security.crypto.EncryptionService;
import id.payu.security.masking.DataMaskingAspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SecurityAutoConfiguration fail-closed defaults (IMP-064).
 */
class SecurityAutoConfigurationTest {

    @Nested
    @SpringBootTest(classes = TestConfiguration.class)
    @ImportAutoConfiguration(SecurityAutoConfiguration.class)
    @TestPropertySource(properties = {
        "payu.security.enabled=true",
        "payu.security.encryption-enabled=true",
        "payu.security.masking-enabled=true",
        "payu.security.audit-enabled=false",
        "payu.security.encryption.password=test-password-for-encryption"
    })
    @DisplayName("All features explicitly enabled")
    class AllFeaturesEnabledTest {

        @Autowired(required = false)
        private EncryptionService encryptionService;

        @Autowired(required = false)
        private DataMaskingAspect maskingAspect;

        @Test
        void testBeansCreatedWhenEnabled() {
            assertThat(encryptionService).isNotNull();
            assertThat(maskingAspect).isNotNull();
        }

        @Test
        void testEncryptionServiceWorks() {
            assertThat(encryptionService).isNotNull();
            String plainText = "test data";
            String encrypted = encryptionService.encrypt(plainText);
            assertThat(encrypted).isNotNull().isNotEqualTo(plainText);

            String decrypted = encryptionService.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        void testDataMaskingAspectExists() {
            assertThat(maskingAspect).isNotNull();
        }
    }

    @Nested
    @SpringBootTest(classes = TestConfiguration.class)
    @ImportAutoConfiguration(SecurityAutoConfiguration.class)
    @TestPropertySource(properties = {
        // No payu.security.* properties set — testing fail-closed defaults
        "placeholder=true"
    })
    @DisplayName("Fail-closed: defaults activate masking without explicit config")
    class FailClosedDefaultsTest {

        @Autowired(required = false)
        private EncryptionService encryptionService;

        @Autowired(required = false)
        private DataMaskingAspect maskingAspect;

        @Test
        @DisplayName("Masking activates by default (matchIfMissing=true)")
        void maskingActivatesByDefault() {
            assertThat(maskingAspect).isNotNull();
        }

        @Test
        @DisplayName("Encryption stays OFF by default (requires key config)")
        void encryptionStaysOffByDefault() {
            assertThat(encryptionService).isNull();
        }
    }

    @Nested
    @SpringBootTest(classes = TestConfiguration.class)
    @ImportAutoConfiguration(SecurityAutoConfiguration.class)
    @TestPropertySource(properties = {
        "payu.security.masking-enabled=false",
        "payu.security.audit-enabled=false"
    })
    @DisplayName("Explicit opt-out overrides fail-closed defaults")
    class ExplicitOptOutTest {

        @Autowired(required = false)
        private DataMaskingAspect maskingAspect;

        @Test
        @DisplayName("Masking disabled when explicitly set to false")
        void maskingDisabledWhenExplicitlyFalse() {
            assertThat(maskingAspect).isNull();
        }
    }

    @Configuration
    static class TestConfiguration {
        // Empty configuration class for @SpringBootTest
    }
}
