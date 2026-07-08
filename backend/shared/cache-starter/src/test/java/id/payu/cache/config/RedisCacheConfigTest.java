package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheConfigTest {

    @Test
    void dataGridRespUsesResp2Handshake() throws Exception {
        RedisCacheConfig config = new RedisCacheConfig(new CacheProperties());

        Method method = RedisCacheConfig.class.getDeclaredMethod("createClientOptions");
        method.setAccessible(true);

        ClientOptions options = (ClientOptions) method.invoke(config);

        assertThat(options.getProtocolVersion()).isEqualTo(ProtocolVersion.RESP2);
        assertThat(options.isPingBeforeActivateConnection()).isFalse();
    }
}
