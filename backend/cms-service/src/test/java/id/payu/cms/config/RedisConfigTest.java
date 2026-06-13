package id.payu.cms.config;

import id.payu.cache.serializer.TypedJsonRedisSerializer;
import id.payu.cms.domain.dto.ContentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("RedisConfig — cache value serializer")
class RedisConfigTest {

    @Test
    @DisplayName("value serializer must round-trip ContentResponse with LocalDate/LocalDateTime")
    void buildValueSerializer_handlesJavaTimeTypes() {
        RedisSerializer<Object> serializer = new TypedJsonRedisSerializer();

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

        Object restored = assertDoesNotThrow(() -> serializer.deserialize(payload));
        assertNotNull(restored);
        ContentResponse typed = assertInstanceOf(ContentResponse.class, restored);
        assertEquals(LocalDate.of(2026, 1, 25), typed.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 31), typed.getEndDate());
        assertEquals(LocalDateTime.of(2026, 1, 24, 10, 30, 0), typed.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 1, 24, 11, 0, 0), typed.getUpdatedAt());
    }

    @Test
    @DisplayName("READY-001: type-erased deserialize must return ContentResponse, not LinkedHashMap")
    void buildValueSerializer_preservesTypeOnTypeErasedDeserialize() {
        RedisSerializer<Object> serializer = new TypedJsonRedisSerializer();

        ContentResponse source = ContentResponse.builder()
            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
            .title("January Promo")
            .contentType("BANNER")
            .startDate(LocalDate.of(2026, 1, 25))
            .endDate(LocalDate.of(2026, 1, 31))
            .createdAt(LocalDateTime.of(2026, 1, 24, 10, 30, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 24, 11, 0, 0))
            .build();

        byte[] payload = assertDoesNotThrow(() -> serializer.serialize(source));

        Object raw = assertDoesNotThrow(() -> serializer.deserialize(payload));
        assertNotNull(raw, "type-erased deserialize must not return null");
        assertInstanceOf(ContentResponse.class, raw,
            "type-erased deserialize must reconstruct ContentResponse, not LinkedHashMap");

        ContentResponse restored = (ContentResponse) raw;
        assertEquals(source.getId(), restored.getId());
        assertEquals(source.getTitle(), restored.getTitle());
        assertEquals(source.getStartDate(), restored.getStartDate());
        assertEquals(source.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    @DisplayName("READY-001: type-erased deserialize must preserve List<ContentResponse>")
    void buildValueSerializer_preservesTypeForListOfContentResponse() {
        RedisSerializer<Object> serializer = new TypedJsonRedisSerializer();

        ContentResponse a = ContentResponse.builder()
            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
            .title("A")
            .build();
        ContentResponse b = ContentResponse.builder()
            .id(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"))
            .title("B")
            .build();
        List<ContentResponse> source = List.of(a, b);

        byte[] payload = assertDoesNotThrow(() -> serializer.serialize(source));

        Object raw = assertDoesNotThrow(() -> serializer.deserialize(payload));
        assertNotNull(raw);
        assertInstanceOf(List.class, raw,
            "type-erased deserialize of List must reconstruct a List container");
        List<?> restored = (List<?>) raw;
        assertEquals(2, restored.size());
        ContentResponse first = assertInstanceOf(ContentResponse.class, restored.get(0),
            "inner ContentResponse element must be reconstructed, not LinkedHashMap");
        ContentResponse second = assertInstanceOf(ContentResponse.class, restored.get(1));
        assertEquals("A", first.getTitle());
        assertEquals("B", second.getTitle());
    }
}

