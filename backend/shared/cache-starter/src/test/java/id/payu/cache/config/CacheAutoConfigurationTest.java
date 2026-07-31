package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CacheAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class));

    @Test
    void shouldNotLoadCacheConfigurationWhenDisabled() {
        contextRunner
                .withPropertyValues("payu.cache.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CacheAutoConfiguration.class);
                });
    }

    @Test
    void cachePropertiesShouldHaveDefaults() {
        CacheProperties properties = new CacheProperties();
        assertThat(properties).isNotNull();
    }

    @Test
    void autoConfigurationMetadataShouldRegisterHotRodCacheConfig() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(
                "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")) {
            assertThat(in).as("starter auto-configuration metadata").isNotNull();
            String metadata = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(metadata)
                    .contains("id.payu.cache.config.CacheAutoConfiguration")
                    .contains("id.payu.cache.config.HotRodCacheConfig");
        }
    }
}
