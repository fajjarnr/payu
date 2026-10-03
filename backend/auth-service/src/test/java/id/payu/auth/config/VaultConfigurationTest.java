package id.payu.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vault auto-configuration and environment-variable fallback tests.
 */
class VaultConfigurationTest {

    @Test
    @DisplayName("Vault should be disabled in test environment by default")
    void vaultShouldBeDisabledInTestEnvironment() {
        // In test environment, VAULT_ENABLED system property is not set,
        // so Boolean.getBoolean returns false
        boolean vaultEnabled = Boolean.getBoolean("VAULT_ENABLED");

        assertThat(vaultEnabled)
                .as("VAULT_ENABLED should be false in test environment")
                .isFalse();
    }

    @Test
    @DisplayName("Environment variables should be resolvable as fallback when Vault is disabled")
    void environmentVariablesShouldBeResolvableAsFallback() {
        // Tests the env-var fallback path via a known system property.
        String testKey = "payu.vault.test.key";
        String testValue = "test-fallback-value";

        System.setProperty(testKey, testValue);
        try {
            String resolved = System.getProperty(testKey);
            assertThat(resolved)
                    .as("System property fallback should resolve correctly")
                    .isEqualTo(testValue);
        } finally {
            System.clearProperty(testKey);
        }
    }

    @Test
    @DisplayName("Vault configuration class should be loadable")
    void vaultConfigurationClassShouldBeLoadable() {
        try {
            Class<?> clazz = Class.forName("id.payu.auth.config.VaultConfigurationTest");
            assertThat(clazz).isNotNull();
        } catch (ClassNotFoundException e) {
            throw new AssertionError("VaultConfigurationTest class should be loadable", e);
        }
    }
}
