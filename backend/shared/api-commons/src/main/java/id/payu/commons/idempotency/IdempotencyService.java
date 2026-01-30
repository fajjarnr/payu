package id.payu.commons.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.api.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for managing idempotency in PayU platform.
 * <p>
 * This service provides thread-safe idempotency handling using Redis storage.
 * It ensures that requests with the same idempotency key are processed exactly once,
 * returning cached responses for duplicate requests.
 * <p>
 * Key features:
 * <ul>
 *   <li>Request fingerprinting for integrity validation</li>
 *   <li>In-progress request detection (prevents concurrent duplicate processing)</li>
 *   <li>Automatic TTL-based cleanup (default 24 hours)</li>
 *   <li>Thread-safe atomic operations</li>
 *   <li>JSON response storage</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>
 * &#64;PostMapping("/transfers")
 * public ResponseEntity&lt;TransferResponse&gt; createTransfer(
 *         &#64;RequestHeader("Idempotency-Key") String idempotencyKey,
 *         &#64;RequestBody TransferRequest request) {
 *
 *     // Check for existing response
 *     Optional&lt;IdempotencyEntry&gt; existing = idempotencyService.get(idempotencyKey, request);
 *     if (existing.isPresent()) {
 *         return ResponseEntity.status(existing.get().getHttpStatus())
 *             .body(objectMapper.readValue(existing.get().getResponseBody(), TransferResponse.class));
 *     }
 *
 *     // Mark as in-progress
 *     idempotencyService.startRequest(idempotencyKey, request);
 *
 *     try {
 *         // Process request
 *         TransferResponse response = transferService.execute(request);
 *
 *         // Store successful response
 *         idempotencyService.storeResponse(idempotencyKey, request, HttpStatus.CREATED, response);
 *         return ResponseEntity.status(HttpStatus.CREATED).body(response);
 *     } catch (Exception e) {
 *         // Store error response
 *         idempotencyService.storeError(idempotencyKey, request, HttpStatus.BAD_REQUEST, e);
 *         throw e;
 *     }
 * }
 * </pre>
 *
 * @see IdempotencyKey
 * @see IdempotencyEntry
 * @see Idempotent
 */
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final int MAX_REQUEST_BODY_SIZE = 1024 * 1024; // 1MB

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves a cached idempotency entry if it exists and matches the request.
     *
     * @param key           the idempotency key string
     * @param requestBody   the request body for fingerprint validation
     * @return Optional containing the entry if found and valid, empty otherwise
     * @throws ConflictException if key exists but request fingerprint doesn't match
     */
    public Optional<IdempotencyEntry> get(String key, Object requestBody) {
        return get(IdempotencyKey.of(key), requestBody);
    }

    /**
     * Retrieves a cached idempotency entry if it exists and matches the request.
     *
     * @param key           the idempotency key
     * @param requestBody   the request body for fingerprint validation
     * @return Optional containing the entry if found and valid, empty otherwise
     * @throws ConflictException if key exists but request fingerprint doesn't match
     */
    public Optional<IdempotencyEntry> get(IdempotencyKey key, Object requestBody) {
        String fingerprint = computeFingerprint(requestBody);
        Optional<IdempotencyEntry> entry = repository.findByKey(key);

        if (entry.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyEntry existing = entry.get();

        // Validate request fingerprint
        if (!existing.matchesFingerprint(fingerprint)) {
            log.warn("Idempotency key '{}' reused with different request body", key.value());
            throw new ConflictException(
                    "IDEMPOTENCY_KEY_REUSE",
                    "Idempotency-Key was already used with a different request body"
            );
        }

        // Check for in-progress requests
        if (existing.isInProgress()) {
            log.warn("Concurrent request detected for idempotency key '{}'", key.value());
            throw new ConflictException(
                    "IDEMPOTENCY_IN_PROGRESS",
                    "A request with this Idempotency-Key is currently being processed"
            );
        }

        log.debug("Returning cached idempotency response for key '{}', status: {}",
                key.value(), existing.getHttpStatus());

        return Optional.of(existing);
    }

    /**
     * Marks a request as in-progress to prevent concurrent duplicate processing.
     *
     * @param key         the idempotency key string
     * @param requestBody the request body for fingerprinting
     * @return true if successfully marked as in-progress, false if already exists
     */
    public boolean startRequest(String key, Object requestBody) {
        return startRequest(IdempotencyKey.of(key), requestBody);
    }

    /**
     * Marks a request as in-progress to prevent concurrent duplicate processing.
     *
     * @param key         the idempotency key
     * @param requestBody the request body for fingerprinting
     * @return true if successfully marked as in-progress, false if already exists
     */
    public boolean startRequest(IdempotencyKey key, Object requestBody) {
        String fingerprint = computeFingerprint(requestBody);
        IdempotencyEntry entry = IdempotencyEntry.inProgress(key.value(), fingerprint);

        boolean saved = repository.saveIfAbsent(key, entry, DEFAULT_TTL.getSeconds());

        if (saved) {
            log.debug("Started idempotency tracking for key '{}'", key.value());
        }

        return saved;
    }

    /**
     * Stores a successful response for idempotency.
     *
     * @param key          the idempotency key string
     * @param requestBody  the original request body
     * @param httpStatus   the HTTP status code
     * @param response     the response object to store
     */
    public void storeResponse(String key, Object requestBody, HttpStatus httpStatus, Object response) {
        storeResponse(IdempotencyKey.of(key), requestBody, httpStatus, response);
    }

    /**
     * Stores a successful response for idempotency.
     *
     * @param key          the idempotency key
     * @param requestBody  the original request body
     * @param httpStatus   the HTTP status code
     * @param response     the response object to store
     */
    public void storeResponse(IdempotencyKey key, Object requestBody, HttpStatus httpStatus, Object response) {
        try {
            String fingerprint = computeFingerprint(requestBody);
            String responseJson = objectMapper.writeValueAsString(response);

            IdempotencyEntry entry = IdempotencyEntry.completed(
                    key.value(),
                    fingerprint,
                    httpStatus.value(),
                    responseJson
            );

            repository.update(key, entry, DEFAULT_TTL.getSeconds());
            log.debug("Stored idempotency response for key '{}', status: {}", key.value(), httpStatus.value());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency key '{}': {}", key.value(), e.getMessage());
            // Don't fail the request because of idempotency storage issues
        }
    }

    /**
     * Stores an error response for idempotency.
     *
     * @param key          the idempotency key string
     * @param requestBody  the original request body
     * @param httpStatus   the HTTP error status code
     * @param error        the error to store
     */
    public void storeError(String key, Object requestBody, HttpStatus httpStatus, Throwable error) {
        storeError(IdempotencyKey.of(key), requestBody, httpStatus, error);
    }

    /**
     * Stores an error response for idempotency.
     *
     * @param key          the idempotency key
     * @param requestBody  the original request body
     * @param httpStatus   the HTTP error status code
     * @param error        the error to store
     */
    public void storeError(IdempotencyKey key, Object requestBody, HttpStatus httpStatus, Throwable error) {
        try {
            String fingerprint = computeFingerprint(requestBody);

            // Create a simple error response
            ErrorResponse errorResponse = new ErrorResponse(
                    error.getClass().getSimpleName(),
                    error.getMessage(),
                    httpStatus.value()
            );
            String errorJson = objectMapper.writeValueAsString(errorResponse);

            IdempotencyEntry entry = IdempotencyEntry.failed(
                    key.value(),
                    fingerprint,
                    httpStatus.value(),
                    errorJson
            );

            repository.update(key, entry, DEFAULT_TTL.getSeconds());
            log.debug("Stored idempotency error for key '{}', status: {}", key.value(), httpStatus.value());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response for idempotency key '{}': {}", key.value(), e.getMessage());
        }
    }

    /**
     * Deletes an idempotency entry.
     *
     * @param key the idempotency key string
     */
    public void delete(String key) {
        delete(IdempotencyKey.of(key));
    }

    /**
     * Deletes an idempotency entry.
     *
     * @param key the idempotency key
     */
    public void delete(IdempotencyKey key) {
        repository.delete(key);
        log.debug("Deleted idempotency entry for key '{}'", key.value());
    }

    /**
     * Checks if an idempotency entry exists.
     *
     * @param key the idempotency key string
     * @return true if entry exists
     */
    public boolean exists(String key) {
        return exists(IdempotencyKey.of(key));
    }

    /**
     * Checks if an idempotency entry exists.
     *
     * @param key the idempotency key
     * @return true if entry exists
     */
    public boolean exists(IdempotencyKey key) {
        return repository.exists(key);
    }

    /**
     * Computes a fingerprint for the request body.
     * <p>
     * The fingerprint is a SHA-256 hash of the serialized request body,
     * used to detect if the same idempotency key is used with different payloads.
     *
     * @param requestBody the request body object
     * @return Base64-encoded SHA-256 hash
     */
    String computeFingerprint(Object requestBody) {
        try {
            String json;
            if (requestBody instanceof String) {
                json = (String) requestBody;
            } else if (requestBody != null) {
                json = objectMapper.writeValueAsString(requestBody);
            } else {
                json = "";
            }

            // Limit size to prevent DoS
            if (json.length() > MAX_REQUEST_BODY_SIZE) {
                log.warn("Request body exceeds maximum size for fingerprinting, truncating");
                json = json.substring(0, MAX_REQUEST_BODY_SIZE);
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request body for fingerprinting: {}", e.getMessage());
            // Return a fallback fingerprint based on class name
            return "fallback:" + (requestBody != null ? requestBody.getClass().getName() : "null");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Simple error response DTO for serialization.
     */
    private record ErrorResponse(String error, String message, int status) {}
}
