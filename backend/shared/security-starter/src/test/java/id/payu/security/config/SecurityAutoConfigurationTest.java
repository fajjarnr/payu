package id.payu.security.config;

import id.payu.security.crypto.EncryptionService;
import id.payu.security.masking.DataMaskingAspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SecurityAutoConfiguration conditional bean creation
 */
@SpringBootTest(classes = SecurityAutoConfigurationTest.TestConfiguration.class)
@ImportAutoConfiguration(SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "payu.security.enabled=true",
    "payu.security.encryption-enabled=true",
    "payu.security.masking-enabled=true",
    "payu.security.encryption.password=test-password-for-encryption"
})
class SecurityAutoConfigurationTest {

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

    @Configuration
    static class TestConfiguration {
        // Empty configuration class for @SpringBootTest
    }
}
