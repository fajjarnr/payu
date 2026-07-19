package id.payu.gateway.adapter.cache;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotRodCacheClientTest {

    @Test
    void shouldUseDigestSha256ByDefault() throws Exception {
        HotRodCacheClient client = configuredClient();
        client.init();
        try {
            assertEquals("DIGEST-SHA-256",
                    cacheManager(client).getConfiguration().security().authentication().saslMechanism());
        } finally {
            client.stop();
        }
    }

    @Test
    void shouldConfigureMutualTls() throws Exception {
        HotRodCacheClient client = configuredClient();
        setField(client, "useSsl", true);
        setField(client, "trustStoreFileName", Optional.of("/var/run/datagrid/truststore.p12"));
        setField(client, "trustStorePassword", Optional.of("changeit"));
        setField(client, "trustStoreType", "PKCS12");
        setField(client, "keyStoreFileName", Optional.of("/var/run/datagrid/client.p12"));
        setField(client, "keyStorePassword", Optional.of("changeit"));
        setField(client, "keyStoreType", "PKCS12");
        setField(client, "keyAlias", Optional.of("payu-client"));
        setField(client, "sniHostName", Optional.of("payu-cache"));

        client.init();
        try {
            var ssl = cacheManager(client).getConfiguration().security().ssl();
            assertEquals(true, ssl.enabled());
            assertEquals("/var/run/datagrid/truststore.p12", ssl.trustStoreFileName());
            assertEquals("/var/run/datagrid/client.p12", ssl.keyStoreFileName());
            assertEquals("payu-client", ssl.keyAlias());
            assertEquals("payu-cache", ssl.sniHostName());
        } finally {
            client.stop();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "datagrid.integration", matches = "true")
    void shouldRoundTripThroughLocalDataGridWithMutualTls() throws Exception {
        HotRodCacheClient client = configuredClient();
        setField(client, "useSsl", true);
        setField(client, "trustStoreFileName", Optional.of(requiredSystemProperty("datagrid.hotrod.trust-store")));
        setField(client, "trustStorePassword", Optional.of("changeit"));
        setField(client, "trustStoreType", "PKCS12");
        setField(client, "keyStoreFileName", Optional.of(requiredSystemProperty("datagrid.hotrod.key-store")));
        setField(client, "keyStorePassword", Optional.of("changeit"));
        setField(client, "keyStoreType", "PKCS12");
        setField(client, "keyAlias", Optional.of("payu-client"));
        setField(client, "sniHostName", Optional.of("localhost"));

        client.init();
        String key = "arch007:gateway:" + UUID.randomUUID();
        try {
            client.put(key, "gateway-value", java.time.Duration.ofSeconds(30)).await().indefinitely();
            assertEquals("gateway-value", client.get(key).await().indefinitely());
        } finally {
            client.remove(key).await().indefinitely();
            client.stop();
        }
    }

    private static HotRodCacheClient configuredClient() throws Exception {
        HotRodCacheClient client = new HotRodCacheClient();
        setField(client, "serverList", "localhost:11222");
        setField(client, "cacheName", "payu");
        setField(client, "username", Optional.of("developer"));
        setField(client, "password", Optional.of("payu-cache-dev-password"));
        setField(client, "realm", "default");
        return client;
    }

    private static RemoteCacheManager cacheManager(HotRodCacheClient client) throws Exception {
        Field field = HotRodCacheClient.class.getDeclaredField("cacheManager");
        field.setAccessible(true);
        return (RemoteCacheManager) field.get(client);
    }

    private static void setField(HotRodCacheClient client, String name, Object value) throws Exception {
        Field field = HotRodCacheClient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(client, value);
    }

    private static String requiredSystemProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing system property " + name);
        }
        return value;
    }
}
