package id.payu.cms.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionMigrationResourcesTest {

    @Test
    void automaticMigrationsMustNotContainSampleContent() throws Exception {
        var migration = new ClassPathResource("db/migration/V1__init.sql");
        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .doesNotContain("Insert sample data for testing")
                .doesNotContain("Welcome Bonus Promo")
                .doesNotContain("Weekend Cashback");
    }
}
