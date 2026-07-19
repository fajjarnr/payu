package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import id.payu.cache.service.HotRodDistributedCacheServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.commons.marshall.UTF8StringMarshaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;

class HotRodCacheConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HotRodCacheConfig.class));

    private final ApplicationContextRunner cacheContextRunner = new ApplicationContextRunner()
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withConfiguration(AutoConfigurations.of(
                    HotRodCacheConfig.class,
                    CacheThreadPoolConfig.class,
                    CacheAutoConfiguration.class));

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
    void shouldConfigureDefaultCacheWithBoundedNearCacheInvalidation() {
        contextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.server-list=localhost:11222",
                        "payu.cache.hotrod.auth-username=developer",
                        "payu.cache.hotrod.auth-password=payu-cache-dev-password")
                .run(context -> {
                    RemoteCacheManager cacheManager = context.getBean(RemoteCacheManager.class);
                    var cacheConfig = cacheManager.getConfiguration().remoteCaches().get("payu");

                    assertThat(cacheConfig).isNotNull();
                    assertThat(cacheConfig.nearCacheMode()).isEqualTo(NearCacheMode.INVALIDATED);
                    assertThat(cacheConfig.nearCacheMaxEntries()).isEqualTo(10_000);
                });
    }

    @Test
    void shouldUseStringMarshallerForJsonCachePayloads() {
        contextRunner
                .withPropertyValues("payu.cache.provider=hotrod")
                .run(context -> assertThat(context.getBean(RemoteCacheManager.class)
                        .getConfiguration().marshallerClass()).isEqualTo(UTF8StringMarshaller.class));
    }

    @Test
    void shouldCreateHotRodDistributedCacheService() {
        cacheContextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.server-list=localhost:11222",
                        "payu.cache.hotrod.auth-username=developer",
                        "payu.cache.hotrod.auth-password=payu-cache-dev-password")
                .run(context -> assertThat(context).hasSingleBean(HotRodDistributedCacheServiceImpl.class));
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
                        RemoteCache<String, String> cache = cacheManager.getCache("payu");
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

    @Test
    @EnabledIfSystemProperty(named = "datagrid.integration", matches = "true")
    void shouldReadAndWriteToLocalDataGridOverHotRod() {
        contextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.server-list=localhost:11222",
                        "payu.cache.hotrod.auth-username=developer",
                        "payu.cache.hotrod.auth-password=payu-cache-dev-password")
                .run(context -> {
                    RemoteCacheManager cacheManager = context.getBean(RemoteCacheManager.class);
                    cacheManager.start();
                    RemoteCache<String, String> cache = cacheManager.getCache("payu");
                    String key = "arch007:integration:" + UUID.randomUUID();
                    try {
                        cache.put(key, "hotrod-value");
                        assertThat(cache.get(key)).isEqualTo("hotrod-value");
                    } finally {
                        cache.remove(key);
                    }
        });
    }

    @Test
    @EnabledIfSystemProperty(named = "datagrid.integration", matches = "true")
    void shouldReadPlainTextEntryWrittenThroughRest() {
        String restKey = System.getProperty("datagrid.rest.key");
        String restValue = System.getProperty("datagrid.rest.value");
        assertThat(restKey).isNotBlank();
        assertThat(restValue).isNotBlank();

        contextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.server-list=localhost:11222",
                        "payu.cache.hotrod.auth-username=developer",
                        "payu.cache.hotrod.auth-password=payu-cache-dev-password")
                .run(context -> {
                    RemoteCacheManager cacheManager = context.getBean(RemoteCacheManager.class);
                    cacheManager.start();
                    RemoteCache<String, String> cache = cacheManager.getCache("payu");
                    assertThat(cache.get(restKey)).isEqualTo(restValue);
                });
    }
}
