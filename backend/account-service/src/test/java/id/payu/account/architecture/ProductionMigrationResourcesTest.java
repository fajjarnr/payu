package id.payu.account.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionMigrationResourcesTest {

    @Test
    void testDataMustNotBePackagedAsAnAutomaticFlywayMigration() {
        assertThat(new ClassPathResource("db/migration/V99__seed_test_data.sql").exists())
                .isFalse();
    }
}
