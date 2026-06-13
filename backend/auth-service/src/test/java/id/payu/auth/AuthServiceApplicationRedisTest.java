package id.payu.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

@DisplayName("AuthServiceApplication — RedisTemplate serializer")
class AuthServiceApplicationRedisTest {

    static final class AuthToken {
        public LocalDate issuedOn;
        public String subject;
        public AuthToken() {}
        public AuthToken(LocalDate issuedOn, String subject) {
            this.issuedOn = issuedOn;
            this.subject = subject;
        }
    }

    @Test
    @DisplayName("value serializer must handle LocalDate without throwing")
    @SuppressWarnings("unchecked")
    void redisTemplate_valueSerializer_handlesLocalDate() {
        AuthServiceApplication app = new AuthServiceApplication();
        RedisConnectionFactory cf = mock(RedisConnectionFactory.class);
        RedisTemplate<String, Object> template = app.redisTemplate(cf);
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();

        AuthToken token = new AuthToken(LocalDate.of(2026, 1, 25), "alice");

        assertDoesNotThrow(() -> serializer.serialize(token));
    }
}
