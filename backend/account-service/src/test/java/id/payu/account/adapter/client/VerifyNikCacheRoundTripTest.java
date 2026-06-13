package id.payu.account.adapter.client;

import id.payu.account.dto.VerifyNikResponse;
import id.payu.cache.serializer.TypedJsonRedisSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * NEW-001 regression test: {@link KycVerificationAdapter#verifyNik} is
 * {@code @Cacheable} on a {@code nikVerification} cache that returns
 * {@link VerifyNikResponse} (single POJO). On the 2nd call with the same NIK,
 * the cache must hit and the deserialized value must be a
 * {@link VerifyNikResponse} — not a {@code LinkedHashMap} (which would
 * cause the Spring proxy to throw {@code ClassCastException}).
 *
 * <p>This test asserts the platform default value serializer (set in
 * {@code cache-starter} by NEW-003) preserves the runtime type on the wire
 * for both single POJOs and (by extension) collection payloads.</p>
 */
@DisplayName("NEW-001 — NIK verification cache deser preserves VerifyNikResponse type")
class VerifyNikCacheRoundTripTest {

    @Test
    @DisplayName("type-erased deserialize of cached VerifyNikResponse reconstructs POJO, not LinkedHashMap")
    void cachedNikVerificationIsTypedAfterRoundTrip() {
        RedisSerializer<Object> serializer = new TypedJsonRedisSerializer();

        VerifyNikResponse source = VerifyNikResponse.success(
            UUID.randomUUID().toString(),
            "3201234567890001",
            true,
            "John Doe",
            "Jakarta",
            LocalDate.of(1990, 1, 15),
            "MALE",
            "Jl. Test No. 123",
            "ACTIVE"
        );

        byte[] payload = assertDoesNotThrow(() -> serializer.serialize(source),
            "serializing a successful NIK verification must not throw");
        assertNotNull(payload);

        Object raw = assertDoesNotThrow(() -> serializer.deserialize(payload),
            "type-erased cache hit must not throw");
        assertNotNull(raw);

        VerifyNikResponse restored = assertInstanceOf(VerifyNikResponse.class, raw,
            "cache hit must reconstruct VerifyNikResponse, not LinkedHashMap "
                + "(this is the NEW-001 bug: ClassCastException on the 2nd verify-nik call)");

        assertEquals(source.nik(), restored.nik());
        assertEquals(source.fullName(), restored.fullName());
        assertEquals(source.birthPlace(), restored.birthPlace());
        assertEquals(source.birthDate(), restored.birthDate());
    }
}
