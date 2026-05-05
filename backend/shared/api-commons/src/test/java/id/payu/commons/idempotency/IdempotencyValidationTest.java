package id.payu.commons.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for idempotency key validation and behavior.
 * Ensures that idempotency is properly enforced across all services.
 */
class IdempotencyValidationTest {

    private IdempotencyService idempotencyService;
    private IdempotencyRepository repository;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        repository = mock(IdempotencyRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        idempotencyService = new IdempotencyService(repository, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void shouldReturnCachedResponseForDuplicateRequest() {
        // Given: A previous request with the same idempotency key
        String idempotencyKey = "550e8400-e29b-41d4-a716-446655440001";
        TestRequest request = new TestRequest("amount", 1000);
        // Compute fingerprint that matches the request
        String expectedFingerprint = idempotencyService.computeFingerprint(request);
        IdempotencyEntry cachedEntry = IdempotencyEntry.completed(
                idempotencyKey,
                expectedFingerprint,
                200,
                "{\"result\":\"success\",\"amount\":1000}"
        );

        when(repository.findByKey(any(IdempotencyKey.class)))
                .thenReturn(Optional.of(cachedEntry));
        when(repository.exists(any(IdempotencyKey.class)))
                .thenReturn(true);

        // When: Making a duplicate request
        Optional<IdempotencyEntry> result = idempotencyService.get(idempotencyKey, request);

        // Then: Should return the cached response
        assertThat(result).isPresent();
        assertThat(result.get().getHttpStatus()).isEqualTo(200);
        assertThat(result.get().getResponseBody()).contains("\"amount\":1000");
    }

    @Test
    void shouldRejectDifferentRequestWithSameKey() {
        // Given: A previous request with different body
        String idempotencyKey = "550e8400-e29b-41d4-a716-446655440002";
        TestRequest originalRequest = new TestRequest("amount", 1000);
        TestRequest differentRequest = new TestRequest("amount", 2000);

        // Use original request fingerprint
        String originalFingerprint = idempotencyService.computeFingerprint(originalRequest);
        IdempotencyEntry cachedEntry = IdempotencyEntry.completed(
                idempotencyKey,
                originalFingerprint,
                200,
                "{\"result\":\"success\"}"
        );

        when(repository.findByKey(any(IdempotencyKey.class)))
                .thenReturn(Optional.of(cachedEntry));

        // When: Making a request with different body
        // Then: Should throw ConflictException
        assertThatThrownBy(() -> idempotencyService.get(idempotencyKey, differentRequest))
                .isInstanceOf(id.payu.api.common.exception.ConflictException.class)
                .hasMessageContaining("Idempotency-Key was already used with a different request body");
    }

    @Test
    void shouldDetectInProgressRequest() {
        // Given: A request currently in progress
        String idempotencyKey = "550e8400-e29b-41d4-a716-446655440003";
        TestRequest request = new TestRequest("action", "transfer");

        // Use matching fingerprint
        String requestFingerprint = idempotencyService.computeFingerprint(request);
        IdempotencyEntry inProgressEntry = IdempotencyEntry.inProgress(
                idempotencyKey,
                requestFingerprint
        );

        when(repository.findByKey(any(IdempotencyKey.class)))
                .thenReturn(Optional.of(inProgressEntry));

        // When: Making a concurrent request
        // Then: Should throw ConflictException for in-progress
        assertThatThrownBy(() -> idempotencyService.get(idempotencyKey, request))
                .isInstanceOf(id.payu.api.common.exception.ConflictException.class)
                .hasMessageContaining("A request with this Idempotency-Key is currently being processed");
    }

    @Test
    void shouldStoreSuccessfulResponse() {
        // Given: A new request
        String idempotencyKey = "550e8400-e29b-41d4-a716-446655440004";
        TestRequest request = new TestRequest("amount", 5000);
        TestResponse response = new TestResponse("success", 5000);

        when(repository.saveIfAbsent(any(IdempotencyKey.class), any(IdempotencyEntry.class), any(Long.class)))
                .thenReturn(true);
        when(repository.exists(any(IdempotencyKey.class)))
                .thenReturn(false);

        // When: Starting a new request
        boolean started = idempotencyService.startRequest(idempotencyKey, request);

        // Then: Should mark as started
        assertThat(started).isTrue();

        // When: Storing the response
        idempotencyService.storeResponse(idempotencyKey, request,
                org.springframework.http.HttpStatus.CREATED, response);

        // Then: Should update repository
        verify(repository).update(any(IdempotencyKey.class), any(IdempotencyEntry.class), any(Long.class));
    }

    @Test
    void shouldStoreErrorResponse() {
        // Given: A request that fails
        String idempotencyKey = "550e8400-e29b-41d4-a716-446655440005";
        TestRequest request = new TestRequest("amount", -1);
        Exception error = new IllegalArgumentException("Invalid amount");

        when(repository.saveIfAbsent(any(IdempotencyKey.class), any(IdempotencyEntry.class), any(Long.class)))
                .thenReturn(true);

        idempotencyService.startRequest(idempotencyKey, request);

        // When: Storing error response
        idempotencyService.storeError(idempotencyKey, request,
                org.springframework.http.HttpStatus.BAD_REQUEST, error);

        // Then: Should update repository with error
        verify(repository).update(any(IdempotencyKey.class), any(IdempotencyEntry.class), any(Long.class));
    }

    @Test
    void shouldComputeConsistentFingerprint() {
        // Given: Two identical request objects
        TestRequest request1 = new TestRequest("from", "ACC001", "to", "ACC002", "amount", 1000);
        TestRequest request2 = new TestRequest("from", "ACC001", "to", "ACC002", "amount", 1000);

        // When: Computing fingerprints
        String fingerprint1 = idempotencyService.computeFingerprint(request1);
        String fingerprint2 = idempotencyService.computeFingerprint(request2);

        // Then: Fingerprints should be identical
        assertThat(fingerprint1).isEqualTo(fingerprint2);
    }

    @Test
    void shouldComputeDifferentFingerprintsForDifferentRequests() {
        // Given: Two different request objects
        TestRequest request1 = new TestRequest("amount", 1000);
        TestRequest request2 = new TestRequest("amount", 2000);

        // When: Computing fingerprints
        String fingerprint1 = idempotencyService.computeFingerprint(request1);
        String fingerprint2 = idempotencyService.computeFingerprint(request2);

        // Then: Fingerprints should be different
        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
    }

    // Test DTOs
    static class TestRequest {
        private final String field1;
        private final Object value1;
        private final String field2;
        private final Object value2;
        private final String field3;
        private final Object value3;

        TestRequest(String field1, Object value1) {
            this.field1 = field1;
            this.value1 = value1;
            this.field2 = null;
            this.value2 = null;
            this.field3 = null;
            this.value3 = null;
        }

        TestRequest(String field1, Object value1, String field2, Object value2) {
            this.field1 = field1;
            this.value1 = value1;
            this.field2 = field2;
            this.value2 = value2;
            this.field3 = null;
            this.value3 = null;
        }

        TestRequest(String field1, Object value1, String field2, Object value2,
                   String field3, Object value3) {
            this.field1 = field1;
            this.value1 = value1;
            this.field2 = field2;
            this.value2 = value2;
            this.field3 = field3;
            this.value3 = value3;
        }

        public String getField1() { return field1; }
        public Object getValue1() { return value1; }
        public String getField2() { return field2; }
        public Object getValue2() { return value2; }
        public String getField3() { return field3; }
        public Object getValue3() { return value3; }
    }

    static class TestResponse {
        private final String status;
        private final int amount;

        TestResponse(String status, int amount) {
            this.status = status;
            this.amount = amount;
        }

        public String getStatus() { return status; }
        public int getAmount() { return amount; }
    }
}
