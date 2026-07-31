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
import org.springframework.cache.CacheManager;

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
    void shouldLoadHotRodConfigByDefaultWhenProviderNotSet() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HotRodCacheConfig.class);
            assertThat(context).hasSingleBean(RemoteCacheManager.class);
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
    void shouldUseDigestSha256ByDefault() {
        contextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.auth-username=developer",
                        "payu.cache.hotrod.auth-password=payu-cache-dev-password")
                .run(context -> assertThat(context.getBean(RemoteCacheManager.class)
                        .getConfiguration().security().authentication().saslMechanism())
                        .isEqualTo("DIGEST-SHA-256"));
    }

    @Test
    void shouldConfigureTlsTrustAndClientKeyStores() {
        contextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.use-ssl=true",
                        "payu.cache.hotrod.sni-host-name=payu-cache.payu-dev.svc",
                        "payu.cache.hotrod.trust-store-file-name=/var/run/datagrid/truststore.p12",
                        "payu.cache.hotrod.trust-store-password=changeit",
                        "payu.cache.hotrod.trust-store-type=PKCS12",
                        "payu.cache.hotrod.key-store-file-name=/var/run/datagrid/client.p12",
                        "payu.cache.hotrod.key-store-password=changeit",
                        "payu.cache.hotrod.key-store-type=PKCS12",
                        "payu.cache.hotrod.key-alias=payu-client")
                .run(context -> {
                    var ssl = context.getBean(RemoteCacheManager.class)
                            .getConfiguration().security().ssl();

                    assertThat(ssl.enabled()).isTrue();
                    assertThat(ssl.sniHostName()).isEqualTo("payu-cache.payu-dev.svc");
                    assertThat(ssl.trustStoreFileName()).isEqualTo("/var/run/datagrid/truststore.p12");
                    assertThat(ssl.trustStoreType()).isEqualTo("PKCS12");
                    assertThat(ssl.keyStoreFileName()).isEqualTo("/var/run/datagrid/client.p12");
                    assertThat(ssl.keyStoreType()).isEqualTo("PKCS12");
                    assertThat(ssl.keyAlias()).isEqualTo("payu-client");
                });
    }

    @Test
    void shouldCreateHotRodDistributedCacheService() {
        cacheContextRunner
                .withPropertyValues(
                        "payu.cache.provider=hotrod",
                        "payu.cache.hotrod.server-list=localhost:11222",
                        "payu.cache.hotrod.auth-username=developer",
                        "payu.cache.hotrod.auth-password=payu-cache-dev-password")
                .run(context -> {
                    assertThat(context).hasSingleBean(HotRodDistributedCacheServiceImpl.class);
                    assertThat(context).hasSingleBean(CacheManager.class);
                });
    }

    @Test
    @EnabledIfSystemProperty(named = "datagrid.integration", matches = "true")
    void shouldReadAndWriteToLocalDataGridOverHotRod() {
        localDataGridContextRunner()
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

        localDataGridContextRunner()
                .run(context -> {
                    RemoteCacheManager cacheManager = context.getBean(RemoteCacheManager.class);
                    cacheManager.start();
                    RemoteCache<String, String> cache = cacheManager.getCache("payu");
                    assertThat(cache.get(restKey)).isEqualTo(restValue);
                });
    }

    private ApplicationContextRunner localDataGridContextRunner() {
        return contextRunner.withPropertyValues(
                "payu.cache.provider=hotrod",
                "payu.cache.hotrod.server-list=" + System.getProperty("datagrid.hotrod.server-list", "localhost:11222"),
                "payu.cache.hotrod.auth-username=developer",
                "payu.cache.hotrod.auth-password=payu-cache-dev-password",
                "payu.cache.hotrod.auth-realm=default",
                "payu.cache.hotrod.sasl-mechanism=DIGEST-SHA-256",
                "payu.cache.hotrod.client-intelligence=BASIC",
                "payu.cache.hotrod.use-ssl=true",
                "payu.cache.hotrod.sni-host-name=localhost",
                "payu.cache.hotrod.trust-store-file-name=" + requiredSystemProperty("datagrid.hotrod.trust-store"),
                "payu.cache.hotrod.trust-store-password=changeit",
                "payu.cache.hotrod.trust-store-type=PKCS12",
                "payu.cache.hotrod.key-store-file-name=" + requiredSystemProperty("datagrid.hotrod.key-store"),
                "payu.cache.hotrod.key-store-password=changeit",
                "payu.cache.hotrod.key-store-type=PKCS12",
                "payu.cache.hotrod.key-alias=payu-client");
    }

    private static String requiredSystemProperty(String name) {
        String value = System.getProperty(name);
        assertThat(value).as("system property %s", name).isNotBlank();
        return value;
    }
}
