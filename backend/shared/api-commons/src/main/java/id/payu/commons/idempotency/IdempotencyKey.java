package id.payu.commons.idempotency;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Value Object representing an Idempotency Key.
 * <p>
 * Idempotency keys are used to ensure that operations are executed exactly once,
 * even if the request is retried due to network issues or timeouts.
 * <p>
 * Key characteristics:
 * <ul>
 *   <li>Immutable and thread-safe</li>
 *   <li>Validates UUID format (standard UUID v4 recommended)</li>
 *   <li>Maximum length: 128 characters</li>
 *   <li>Case-insensitive comparison</li>
 * </ul>
 *
 * @see IdempotencyService
 * @see Idempotent
 */
@Getter
@EqualsAndHashCode
@ToString
public final class IdempotencyKey {

    private static final int MAX_LENGTH = 128;
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private final String value;

    private IdempotencyKey(String value) {
        this.value = value.toLowerCase();
    }

    /**
     * Creates an IdempotencyKey from a string value.
     *
     * @param value the idempotency key string (must be valid UUID format)
     * @return a new IdempotencyKey instance
     * @throws IllegalArgumentException if value is null, empty, too long, or invalid format
     */
    public static IdempotencyKey of(String value) {
        validate(value);
        return new IdempotencyKey(value);
    }

    /**
     * Creates a new random IdempotencyKey using UUID v4.
     *
     * @return a new randomly generated IdempotencyKey
     */
    public static IdempotencyKey generate() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }

    /**
     * Validates the idempotency key format.
     *
     * @param value the key to validate
     * @throws IllegalArgumentException if validation fails
     */
    private static void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Idempotency key exceeds maximum length of %d characters", MAX_LENGTH)
            );
        }

        // Strict validation: require UUID format
        if (!UUID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency key must be a valid UUID format (e.g., 550e8400-e29b-41d4-a716-446655440000)"
            );
        }
    }

    /**
     * Returns the raw string value of this key.
     *
     * @return the key value (always lowercase)
     */
    public String value() {
        return value;
    }

    /**
     * Returns the Redis cache key for this idempotency key.
     *
     * @return formatted cache key
     */
    public String toCacheKey() {
        return "idempotency:" + value;
    }

    /**
     * Returns the Redis cache key for storing the request fingerprint.
     *
     * @return formatted fingerprint cache key
     */
    public String toFingerprintKey() {
        return "idempotency:" + value + ":fingerprint";
    }
}
