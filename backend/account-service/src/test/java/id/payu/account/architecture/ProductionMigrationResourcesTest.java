package id.payu.account.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionMigrationResourcesTest {

    @Test
    void testDataMustNotBePackagedAsAnAutomaticFlywayMigration() {
        assertThat(new ClassPathResource("db/migration/V99__seed_test_data.sql").exists())
                .isFalse();
    }

    @Test
    void beneficiariesMigrationMustAddTenantIsolation() throws IOException {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V104__add_tenant_to_beneficiaries.sql");

        assertThat(migration.exists()).isTrue();
        assertThat(migration.getContentAsString(StandardCharsets.UTF_8))
                .contains("ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default'")
                .contains("idx_beneficiaries_tenant_id");
    }
}
