package id.payu.commons.idempotency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a stored idempotency entry.
 * <p>
 * This class stores the result of an idempotent request, including:
 * <ul>
 *   <li>The original request fingerprint (for validation)</li>
 *   <li>The HTTP status code of the response</li>
 *   <li>The response body (as JSON string)</li>
 *   <li>Response headers (optional)</li>
 *   <li>Timestamp of when the entry was created</li>
 * </ul>
 * <p>
 * Entries are stored in Redis with a TTL (default 24 hours) and are automatically
 * cleaned up after expiration.
 *
 * @see IdempotencyService
 * @see IdempotencyKey
 */
@Getter
@EqualsAndHashCode
@ToString(exclude = "responseBody")
public final class IdempotencyEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String idempotencyKey;
    private final String requestFingerprint;
    private final int httpStatus;
    private final String responseBody;
    private final String contentType;
    private final Instant createdAt;
    private final EntryStatus status;

    /**
     * Status of the idempotency entry.
     */
    public enum EntryStatus {
        /**
         * Request is currently being processed (in-flight).
         */
        IN_PROGRESS,

        /**
         * Request has been completed successfully.
         */
        COMPLETED,

        /**
         * Request failed with an error.
         */
        FAILED
    }

    /**
     * Creates a new IdempotencyEntry.
     *
     * @param idempotencyKey     the idempotency key
     * @param requestFingerprint fingerprint of the original request
     * @param httpStatus         HTTP status code of the response
     * @param responseBody       response body as JSON string
     * @param contentType        content type of the response
     * @param createdAt          timestamp when entry was created
     * @param status             status of the entry
     */
    @JsonCreator
    @Builder
    public IdempotencyEntry(
            @JsonProperty("idempotencyKey") String idempotencyKey,
            @JsonProperty("requestFingerprint") String requestFingerprint,
            @JsonProperty("httpStatus") int httpStatus,
            @JsonProperty("responseBody") String responseBody,
            @JsonProperty("contentType") String contentType,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("status") EntryStatus status) {

        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey cannot be null");
        this.requestFingerprint = Objects.requireNonNull(requestFingerprint, "requestFingerprint cannot be null");
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.contentType = contentType != null ? contentType : "application/json";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.status = status != null ? status : EntryStatus.COMPLETED;
    }

    /**
     * Creates an entry for an in-progress request.
     *
     * @param idempotencyKey     the idempotency key
     * @param requestFingerprint fingerprint of the request
     * @return a new IN_PROGRESS entry
     */
    public static IdempotencyEntry inProgress(String idempotencyKey, String requestFingerprint) {
        return IdempotencyEntry.builder()
                .idempotencyKey(idempotencyKey)
                .requestFingerprint(requestFingerprint)
                .httpStatus(202) // Accepted
                .status(EntryStatus.IN_PROGRESS)
                .responseBody(null)
                .build();
    }

    /**
     * Creates an entry for a completed request.
     *
     * @param idempotencyKey     the idempotency key
     * @param requestFingerprint fingerprint of the request
     * @param httpStatus         HTTP status code
     * @param responseBody       response body as JSON
     * @return a new COMPLETED entry
     */
    public static IdempotencyEntry completed(
            String idempotencyKey,
            String requestFingerprint,
            int httpStatus,
            String responseBody) {

        return IdempotencyEntry.builder()
                .idempotencyKey(idempotencyKey)
                .requestFingerprint(requestFingerprint)
                .httpStatus(httpStatus)
                .responseBody(responseBody)
                .status(EntryStatus.COMPLETED)
                .build();
    }

    /**
     * Creates an entry for a failed request.
     *
     * @param idempotencyKey     the idempotency key
     * @param requestFingerprint fingerprint of the request
     * @param httpStatus         HTTP error status code
     * @param errorBody          error response body as JSON
     * @return a new FAILED entry
     */
    public static IdempotencyEntry failed(
            String idempotencyKey,
            String requestFingerprint,
            int httpStatus,
            String errorBody) {

        return IdempotencyEntry.builder()
                .idempotencyKey(idempotencyKey)
                .requestFingerprint(requestFingerprint)
                .httpStatus(httpStatus)
                .responseBody(errorBody)
                .status(EntryStatus.FAILED)
                .build();
    }

    /**
     * Checks if this entry represents an in-progress request.
     *
     * @return true if status is IN_PROGRESS
     */
    public boolean isInProgress() {
        return status == EntryStatus.IN_PROGRESS;
    }

    /**
     * Checks if this entry represents a completed request.
     *
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return status == EntryStatus.COMPLETED;
    }

    /**
     * Checks if this entry represents a failed request.
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return status == EntryStatus.FAILED;
    }

    /**
     * Validates that the request fingerprint matches.
     *
     * @param fingerprint the fingerprint to check
     * @return true if fingerprints match
     */
    public boolean matchesFingerprint(String fingerprint) {
        return this.requestFingerprint.equals(fingerprint);
    }
}
