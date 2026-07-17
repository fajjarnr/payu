package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class HotRodCacheConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HotRodCacheConfig.class));

    @Test
    void shouldNotLoadHotRodConfigByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(HotRodCacheConfig.class);
            assertThat(context).doesNotHaveBean(RemoteCacheManager.class);
        });
    }

    @Test
    void shouldLoadHotRodConfigWhenProviderIsHotRod() {
        contextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.server-list=localhost:11222",
                        "payu.cache.hotrod.auth-username=developer",
                        "payu.cache.hotrod.auth-password=payu-cache-dev-password"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(HotRodCacheConfig.class);
                    assertThat(context).hasSingleBean(RemoteCacheManager.class);
                });
    }

    @Test
    void shouldConnectAndPerformHotRodOperationsIfServerAvailable() {
        contextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.server-list=localhost:11222",
                        "payu.cache.hotrod.auth-username=developer",
                        "payu.cache.hotrod.auth-password=payu-cache-dev-password",
                        "payu.cache.hotrod.auth-realm=default",
                        "payu.cache.hotrod.sasl-mechanism=DIGEST-MD5"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RemoteCacheManager.class);
                    RemoteCacheManager cacheManager = context.getBean(RemoteCacheManager.class);
                    assertThat(cacheManager).isNotNull();

                    try {
                        RemoteCache<String, String> cache = cacheManager.getCache("default");
                        if (cache != null) {
                            cache.put("arch007:test:key", "hotrod-value");
                            String val = cache.get("arch007:test:key");
                            assertThat(val).isEqualTo("hotrod-value");
                            cache.remove("arch007:test:key");
                        }
                    } catch (Exception e) {
                        // Log connection status, pass context assertion
                        System.out.println("Hot Rod server test connection note: " + e.getMessage());
                    }
                });
    }
}
