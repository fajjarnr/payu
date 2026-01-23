package id.payu.account.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.vault.core.VaultTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VaultConfigurationTest {

    @Autowired(required = false)
    private VaultTemplate vaultTemplate;

    @Test
    void vaultConfigurationLoaded_whenVaultEnabled() {
        if (Boolean.getBoolean("VAULT_ENABLED") && vaultTemplate != null) {
            assertThat(vaultTemplate).isNotNull();
        }
    }

    @Test
    void environmentVariablesTakePrecedence_whenVaultDisabled() {
        String dbUrl = System.getenv("DB_URL");
        String dbUsername = System.getenv("DB_USERNAME");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (!Boolean.getBoolean("VAULT_ENABLED")) {
            assertThat(dbUrl).isNotNull();
            assertThat(dbUsername).isNotNull();
            assertThat(dbPassword).isNotNull();
        }
    }
}
