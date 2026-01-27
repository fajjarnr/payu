package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
}
