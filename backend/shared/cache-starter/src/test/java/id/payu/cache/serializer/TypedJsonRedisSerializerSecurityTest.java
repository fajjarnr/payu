package id.payu.cache.serializer;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypedJsonRedisSerializerSecurityTest {

    private final TypedJsonRedisSerializer serializer = new TypedJsonRedisSerializer();

    @Test
    void shouldRejectArbitraryClassHeader() {
        // GAP-34 fix: java.net.URL has a static initializer that performs DNS resolution.
        // RCE/gadget vector if attacker can write to Redis with crafted type header.
        String malicious = "java.net.URL|\"http://attacker.example/\"";
        byte[] payload = malicious.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> serializer.deserialize(payload))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("whitelist");
    }

    @Test
    void shouldRejectCollectionElementOutsideWhitelist() {
        // Element type outside whitelist must also be rejected.
        String malicious = "java.util.ArrayList<java.net.URL>|[\"http://attacker/\"]";
        byte[] payload = malicious.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> serializer.deserialize(payload))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("whitelist");
    }

    @Test
    void shouldAcceptWhitelistedClassHeader() {
        // Sanity: legitimate java.util.* types still deserialize.
        String json = "[]";
        String allowed = "java.util.ArrayList|" + json;
        byte[] payload = allowed.getBytes(StandardCharsets.UTF_8);

        Object result = serializer.deserialize(payload);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldRejectEmptyHeader() {
        byte[] payload = "|{}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> serializer.deserialize(payload))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void shouldRejectOverlongHeader() {
        String longName = "id.payu." + "a".repeat(300);
        byte[] payload = (longName + "|{}").getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> serializer.deserialize(payload))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("too long");
    }
}
