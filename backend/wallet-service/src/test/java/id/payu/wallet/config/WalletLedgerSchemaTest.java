package id.payu.wallet.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WalletLedgerSchemaTest {

    @Test
    void supportsVirtualAccountIdentifiers() throws IOException {
        try (InputStream migration = getClass().getClassLoader().getResourceAsStream(
                "db/migration/V111__expand_ledger_account_id.sql")) {
            assertThat(migration).as("ledger account_id widening migration").isNotNull();
            String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("ALTER COLUMN account_id TYPE VARCHAR(128)");
        }
    }
}
