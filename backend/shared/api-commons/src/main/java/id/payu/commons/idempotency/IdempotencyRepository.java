package id.payu.commons.idempotency;

import java.util.Optional;

/**
 * Repository interface for idempotency entry storage.
 * <p>
 * This port defines the contract for storing and retrieving idempotency entries.
 * Implementations should use Redis or similar key-value store with TTL support.
 * <p>
 * Design principles:
 * <ul>
 *   <li>Thread-safe operations</li>
 *   <li>Atomic check-and-set for conflict detection</li>
 *   <li>TTL-based automatic expiration</li>
 *   <li>Consistent serialization format (JSON)</li>
 * </ul>
 *
 * @see IdempotencyEntry
 * @see IdempotencyService
 */
public interface IdempotencyRepository {

    /**
     * Finds an idempotency entry by its key.
     *
     * @param key the idempotency key
     * @return Optional containing the entry if found, empty otherwise
     */
    Optional<IdempotencyEntry> findByKey(IdempotencyKey key);

    /**
     * Saves an idempotency entry with the specified TTL.
     * <p>
     * If an entry already exists with the same key, it should be overwritten
     * only if the request fingerprint matches (same request replay).
     *
     * @param key     the idempotency key
     * @param entry   the entry to save
     * @param ttlSeconds TTL in seconds
     * @return true if saved successfully, false if conflict detected
     */
    boolean save(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds);

    /**
     * Atomically saves an entry only if no entry exists for the key.
     * <p>
     * This is used to establish an in-progress lock for the request.
     *
     * @param key        the idempotency key
     * @param entry      the entry to save
     * @param ttlSeconds TTL in seconds
     * @return true if saved (no existing entry), false if entry already exists
     */
    boolean saveIfAbsent(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds);

    /**
     * Updates an existing entry.
     * <p>
     * Used to transition from IN_PROGRESS to COMPLETED/FAILED status.
     *
     * @param key        the idempotency key
     * @param entry      the updated entry
     * @param ttlSeconds TTL in seconds
     */
    void update(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds);

    /**
     * Deletes an idempotency entry.
     *
     * @param key the idempotency key
     */
    void delete(IdempotencyKey key);

    /**
     * Checks if an entry exists for the given key.
     *
     * @param key the idempotency key
     * @return true if entry exists
     */
    boolean exists(IdempotencyKey key);

    /**
     * Gets the remaining TTL for an entry.
     *
     * @param key the idempotency key
     * @return remaining TTL in seconds, or -1 if not found or no TTL
     */
    long getTtl(IdempotencyKey key);
}
