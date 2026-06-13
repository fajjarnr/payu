package id.payu.cms.config;

import id.payu.cms.domain.dto.ContentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("RedisConfig — cache value serializer")
class RedisConfigTest {

    @Test
    @DisplayName("value serializer must round-trip ContentResponse with LocalDate/LocalDateTime")
    void buildValueSerializer_handlesJavaTimeTypes() {
        GenericJackson2JsonRedisSerializer serializer = new RedisConfig().buildValueSerializer();

        ContentResponse source = ContentResponse.builder()
            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
            .title("January Promo")
            .startDate(LocalDate.of(2026, 1, 25))
            .endDate(LocalDate.of(2026, 1, 31))
            .createdAt(LocalDateTime.of(2026, 1, 24, 10, 30, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 24, 11, 0, 0))
            .build();

        byte[] payload = assertDoesNotThrow(() -> serializer.serialize(source));
        assertNotNull(payload);
        assertEquals(0, payload.length == 0 ? 1 : 0, "serialized payload should not be empty");

        ContentResponse restored = serializer.deserialize(payload, ContentResponse.class);
        assertNotNull(restored);
        assertEquals(LocalDate.of(2026, 1, 25), restored.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 31), restored.getEndDate());
        assertEquals(LocalDateTime.of(2026, 1, 24, 10, 30, 0), restored.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 1, 24, 11, 0, 0), restored.getUpdatedAt());
    }
}
