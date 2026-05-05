package id.payu.commons.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.api.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IdempotencyService}.
 *
 * Test scenarios:
 * - Idempotency key validation
 * - Request fingerprinting
 * - Cached response retrieval
 * - Fingerprint mismatch detection
 * - Concurrent request handling
 * - Response storage
 * - Error response storage
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String VALID_KEY = "550e8400-e29b-41d4-a716-446655440000";
    private static final String DIFFERENT_KEY = "660e8400-e29b-41d4-a716-446655440001";

    @Mock
    private IdempotencyRepository repository;

    private ObjectMapper objectMapper;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new IdempotencyService(repository, objectMapper);
    }

    // ==================== IdempotencyKey Tests ====================

    @Test
    @DisplayName("Should create IdempotencyKey from valid UUID")
    void shouldCreateIdempotencyKeyFromValidUuid() {
        IdempotencyKey key = IdempotencyKey.of(VALID_KEY);

        assertThat(key.value()).isEqualTo(VALID_KEY.toLowerCase());
        assertThat(key.toCacheKey()).isEqualTo("idempotency:" + VALID_KEY.toLowerCase());
    }

    @Test
    @DisplayName("Should generate random IdempotencyKey")
    void shouldGenerateRandomIdempotencyKey() {
        IdempotencyKey key = IdempotencyKey.generate();

        assertThat(key.value()).isNotNull();
        assertThat(key.value()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    @DisplayName("Should reject null idempotency key")
    void shouldRejectNullIdempotencyKey() {
        assertThatThrownBy(() -> IdempotencyKey.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Should reject empty idempotency key")
    void shouldRejectEmptyIdempotencyKey() {
        assertThatThrownBy(() -> IdempotencyKey.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("Should reject invalid UUID format")
    void shouldRejectInvalidUuidFormat() {
        assertThatThrownBy(() -> IdempotencyKey.of("invalid-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUID format");
    }

    @Test
    @DisplayName("Should reject idempotency key exceeding max length")
    void shouldRejectKeyExceedingMaxLength() {
        String longKey = "a".repeat(129);
        assertThatThrownBy(() -> IdempotencyKey.of(longKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum length");
    }

    @Test
    @DisplayName("Should be case-insensitive for UUID keys")
    void shouldBeCaseInsensitive() {
        IdempotencyKey lowerCase = IdempotencyKey.of(VALID_KEY.toLowerCase());
        IdempotencyKey upperCase = IdempotencyKey.of(VALID_KEY.toUpperCase());

        assertThat(lowerCase).isEqualTo(upperCase);
        assertThat(lowerCase.value()).isEqualTo(upperCase.value());
    }

    // ==================== Request Fingerprint Tests ====================

    @Test
    @DisplayName("Should compute consistent fingerprint for same request")
    void shouldComputeConsistentFingerprint() {
        TestRequest request = new TestRequest("amount", 100.0);

        String fingerprint1 = service.computeFingerprint(request);
        String fingerprint2 = service.computeFingerprint(request);

        assertThat(fingerprint1).isEqualTo(fingerprint2);
    }

    @Test
    @DisplayName("Should compute different fingerprints for different requests")
    void shouldComputeDifferentFingerprints() {
        TestRequest request1 = new TestRequest("amount", 100.0);
        TestRequest request2 = new TestRequest("amount", 200.0);

        String fingerprint1 = service.computeFingerprint(request1);
        String fingerprint2 = service.computeFingerprint(request2);

        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
    }

    @Test
    @DisplayName("Should handle string request body for fingerprinting")
    void shouldHandleStringRequestBody() {
        String requestBody = "{\"amount\":100.0}";

        String fingerprint = service.computeFingerprint(requestBody);

        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle null request body for fingerprinting")
    void shouldHandleNullRequestBody() {
        String fingerprint = service.computeFingerprint(null);

        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).isNotEmpty();
    }

    // ==================== Get Entry Tests ====================

    @Test
    @DisplayName("Should return empty when no entry exists")
    void shouldReturnEmptyWhenNoEntryExists() {
        when(repository.findByKey(any(IdempotencyKey.class))).thenReturn(Optional.empty());

        Optional<IdempotencyEntry> result = service.get(VALID_KEY, new TestRequest("test", 1.0));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return cached entry when found and fingerprint matches")
    void shouldReturnCachedEntryWhenFingerprintMatches() {
        TestRequest request = new TestRequest("amount", 100.0);
        String fingerprint = service.computeFingerprint(request);

        IdempotencyEntry entry = IdempotencyEntry.completed(
                VALID_KEY, fingerprint, 201, "{\"id\":\"123\"}"
        );

        when(repository.findByKey(any(IdempotencyKey.class))).thenReturn(Optional.of(entry));

        Optional<IdempotencyEntry> result = service.get(VALID_KEY, request);

        assertThat(result).isPresent();
        assertThat(result.get().getHttpStatus()).isEqualTo(201);
        assertThat(result.get().getResponseBody()).isEqualTo("{\"id\":\"123\"}");
    }

    @Test
    @DisplayName("Should throw ConflictException when fingerprint doesn't match")
    void shouldThrowConflictWhenFingerprintMismatch() {
        TestRequest request1 = new TestRequest("amount", 100.0);
        TestRequest request2 = new TestRequest("amount", 200.0);

        String fingerprint1 = service.computeFingerprint(request1);
        String fingerprint2 = service.computeFingerprint(request2);

        IdempotencyEntry entry = IdempotencyEntry.completed(
                VALID_KEY, fingerprint1, 201, "{\"id\":\"123\"}"
        );

        when(repository.findByKey(any(IdempotencyKey.class))).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.get(VALID_KEY, request2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already used with a different request body");
    }

    @Test
    @DisplayName("Should throw ConflictException when request is in progress")
    void shouldThrowConflictWhenRequestInProgress() {
        TestRequest request = new TestRequest("amount", 100.0);
        String fingerprint = service.computeFingerprint(request);

        IdempotencyEntry entry = IdempotencyEntry.inProgress(VALID_KEY, fingerprint);

        when(repository.findByKey(any(IdempotencyKey.class))).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.get(VALID_KEY, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("currently being processed");
    }

    // ==================== Start Request Tests ====================

    @Test
    @DisplayName("Should successfully start new request")
    void shouldStartNewRequest() {
        TestRequest request = new TestRequest("amount", 100.0);

        when(repository.saveIfAbsent(any(IdempotencyKey.class), any(IdempotencyEntry.class), anyLong()))
                .thenReturn(true);

        boolean started = service.startRequest(VALID_KEY, request);

        assertThat(started).isTrue();
        verify(repository).saveIfAbsent(
                argThat(key -> key.value().equals(VALID_KEY)),
                argThat(entry -> entry.isInProgress()),
                eq(86400L) // 24 hours in seconds
        );
    }

    @Test
    @DisplayName("Should fail to start when entry already exists")
    void shouldFailToStartWhenEntryExists() {
        TestRequest request = new TestRequest("amount", 100.0);

        when(repository.saveIfAbsent(any(IdempotencyKey.class), any(IdempotencyEntry.class), anyLong()))
                .thenReturn(false);

        boolean started = service.startRequest(VALID_KEY, request);

        assertThat(started).isFalse();
    }

    // ==================== Store Response Tests ====================

    @Test
    @DisplayName("Should store successful response")
    void shouldStoreSuccessfulResponse() {
        TestRequest request = new TestRequest("amount", 100.0);
        TestResponse response = new TestResponse("123", "success");

        service.storeResponse(VALID_KEY, request, HttpStatus.CREATED, response);

        verify(repository).update(
                argThat(key -> key.value().equals(VALID_KEY)),
                argThat(entry ->
                        entry.isCompleted() &&
                        entry.getHttpStatus() == 201 &&
                        entry.getResponseBody().contains("123")
                ),
                eq(86400L)
        );
    }

    @Test
    @DisplayName("Should handle serialization error gracefully when storing response")
    void shouldHandleSerializationErrorGracefully() {
        // Create a request that can't be serialized (circular reference)
        Object badRequest = new Object() {
            @SuppressWarnings("unused")
            public Object getSelf() { return this; }
        };

        // This should not throw
        service.storeResponse(VALID_KEY, badRequest, HttpStatus.OK, new TestResponse("1", "ok"));

        // Repository should still be called with the response
        verify(repository).update(any(), any(), anyLong());
    }

    // ==================== Store Error Tests ====================

    @Test
    @DisplayName("Should store error response")
    void shouldStoreErrorResponse() {
        TestRequest request = new TestRequest("amount", 100.0);
        Exception error = new RuntimeException("Something went wrong");

        service.storeError(VALID_KEY, request, HttpStatus.BAD_REQUEST, error);

        verify(repository).update(
                argThat(key -> key.value().equals(VALID_KEY)),
                argThat(entry ->
                        entry.isFailed() &&
                        entry.getHttpStatus() == 400
                ),
                eq(86400L)
        );
    }

    // ==================== Delete Tests ====================

    @Test
    @DisplayName("Should delete idempotency entry")
    void shouldDeleteEntry() {
        service.delete(VALID_KEY);

        verify(repository).delete(argThat(key -> key.value().equals(VALID_KEY)));
    }

    // ==================== Exists Tests ====================

    @Test
    @DisplayName("Should check if entry exists")
    void shouldCheckIfEntryExists() {
        when(repository.exists(any(IdempotencyKey.class))).thenReturn(true);

        boolean exists = service.exists(VALID_KEY);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when entry does not exist")
    void shouldReturnFalseWhenEntryNotExists() {
        when(repository.exists(any(IdempotencyKey.class))).thenReturn(false);

        boolean exists = service.exists(VALID_KEY);

        assertThat(exists).isFalse();
    }

    // ==================== IdempotencyEntry Tests ====================

    @Test
    @DisplayName("Should create in-progress entry")
    void shouldCreateInProgressEntry() {
        IdempotencyEntry entry = IdempotencyEntry.inProgress(VALID_KEY, "fingerprint123");

        assertThat(entry.isInProgress()).isTrue();
        assertThat(entry.isCompleted()).isFalse();
        assertThat(entry.isFailed()).isFalse();
        assertThat(entry.getIdempotencyKey()).isEqualTo(VALID_KEY);
        assertThat(entry.getRequestFingerprint()).isEqualTo("fingerprint123");
    }

    @Test
    @DisplayName("Should create completed entry")
    void shouldCreateCompletedEntry() {
        IdempotencyEntry entry = IdempotencyEntry.completed(
                VALID_KEY, "fingerprint123", 201, "{\"id\":\"123\"}"
        );

        assertThat(entry.isCompleted()).isTrue();
        assertThat(entry.isInProgress()).isFalse();
        assertThat(entry.getHttpStatus()).isEqualTo(201);
        assertThat(entry.getResponseBody()).isEqualTo("{\"id\":\"123\"}");
    }

    @Test
    @DisplayName("Should create failed entry")
    void shouldCreateFailedEntry() {
        IdempotencyEntry entry = IdempotencyEntry.failed(
                VALID_KEY, "fingerprint123", 400, "{\"error\":\"Bad Request\"}"
        );

        assertThat(entry.isFailed()).isTrue();
        assertThat(entry.isCompleted()).isFalse();
        assertThat(entry.getHttpStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should match fingerprint correctly")
    void shouldMatchFingerprint() {
        IdempotencyEntry entry = IdempotencyEntry.completed(
                VALID_KEY, "fingerprint123", 200, "{}"
        );

        assertThat(entry.matchesFingerprint("fingerprint123")).isTrue();
        assertThat(entry.matchesFingerprint("different")).isFalse();
    }

    // ==================== Test Data Classes ====================

    private record TestRequest(String field, Double amount) {}

    private record TestResponse(String id, String status) {}
}
