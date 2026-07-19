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

    @Test
    void cacheConfigurationMustUseTheSharedHotRodContract() throws Exception {
        var configuration = new ClassPathResource("application.yml");
        String yaml = configuration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("provider: ${PAYU_CACHE_PROVIDER:hotrod}")
                .contains("server-list: ${PAYU_CACHE_HOTROD_SERVER_LIST:localhost:11222}")
                .contains("cache-name: ${PAYU_CACHE_HOTROD_CACHE_NAME:payu}")
                .doesNotContain("provider: ${PAYU_CACHE_PROVIDER:resp}")
                .doesNotContain("spring:\n  data:\n    redis:")
                .doesNotContain("cache:\n    type: redis");
    }
}
