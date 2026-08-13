package id.payu.outbox.repository;

import id.payu.outbox.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link OutboxEvent} entities.
 * <p>
 * Provides methods for querying and managing outbox events, including
 * optimistic/pessimistic locking support for concurrent publisher instances.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Finds all events for an aggregate (used by atomicity/audit tests).
     */
    java.util.List<OutboxEvent> findByAggregateId(String aggregateId);

    /**
     * Finds all unpublished events (events where published_at is null)
     * ordered by sequence number for strict ordering guarantees.
     *
     * @param pageable pagination information
     * @return a page of unpublished outbox events
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NULL ORDER BY o.sequenceNum ASC")
    Page<OutboxEvent> findUnpublishedEvents(Pageable pageable);

    /**
     * Finds all unpublished events with retry count less than the specified maximum.
     * Used by the publisher to fetch events that are eligible for publishing.
     *
     * @param maxRetries the maximum number of retry attempts
     * @param pageable pagination information
     * @return a page of unpublished outbox events eligible for retry
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NULL AND o.retryCount < :maxRetries ORDER BY o.sequenceNum ASC")
    Page<OutboxEvent> findUnpublishedEventsForRetry(@Param("maxRetries") int maxRetries, Pageable pageable);

    /**
     * Finds unpublished events with pessimistic locking to prevent concurrent processing
     * by multiple publisher instances.
     * <p>
     * BUG-BE-100: Uses native query with FOR UPDATE SKIP LOCKED (PostgreSQL 9.5+)
     * so multiple publisher pods can each grab different rows without blocking.
     * Without SKIP LOCKED, @Lock(PESSIMISTIC_WRITE) causes all pods to contend
     * on the same rows, defeating the purpose of multi-pod scaling.
     *
     * @param maxRetries the maximum number of retry attempts
     * @param limit the maximum number of events to fetch
     * @return a list of unpublished outbox events locked for update
     */
    @Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL AND retry_count < :maxRetries " +
            "ORDER BY sequence_num ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findUnpublishedEventsWithLock(@Param("maxRetries") int maxRetries, @Param("limit") int limit);

    /**
     * Finds events by aggregate type and aggregate ID.
     * Useful for querying the event history of a specific aggregate.
     *
     * @param aggregateType the type of aggregate
     * @param aggregateId the ID of the aggregate instance
     * @param pageable pagination information
     * @return a page of outbox events for the specified aggregate
     */
    Page<OutboxEvent> findByAggregateTypeAndAggregateIdOrderBySequenceNumAsc(
            String aggregateType, String aggregateId, Pageable pageable);

    Optional<OutboxEvent> findFirstByAggregateTypeAndAggregateIdAndEventType(
            String aggregateType, String aggregateId, String eventType);

    /**
     * Finds events by aggregate type.
     *
     * @param aggregateType the type of aggregate
     * @param pageable pagination information
     * @return a page of outbox events for the specified aggregate type
     */
    Page<OutboxEvent> findByAggregateTypeOrderBySequenceNumAsc(String aggregateType, Pageable pageable);

    /**
     * Finds events by event type.
     *
     * @param eventType the type of event
     * @param pageable pagination information
     * @return a page of outbox events of the specified type
     */
    Page<OutboxEvent> findByEventTypeOrderByCreatedAtAsc(String eventType, Pageable pageable);

    /**
     * Counts the number of unpublished events.
     *
     * @return the count of unpublished events
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.publishedAt IS NULL")
    long countUnpublishedEvents();

    /**
     * Counts the number of unpublished events that have exceeded the maximum retry count.
     *
     * @param maxRetries the maximum number of retry attempts
     * @return the count of failed events
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.publishedAt IS NULL AND o.retryCount >= :maxRetries")
    long countFailedEvents(@Param("maxRetries") int maxRetries);

    /**
     * Marks an event as published by setting the published_at timestamp.
     *
     * @param id the ID of the event to mark as published
     * @param publishedAt the timestamp when the event was published
     * @return the number of rows affected
     */
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.publishedAt = :publishedAt WHERE o.id = :id AND o.publishedAt IS NULL")
    int markAsPublished(@Param("id") UUID id, @Param("publishedAt") Instant publishedAt);

    /**
     * Increments the retry count and sets the error message for an event.
     *
     * @param id the ID of the event
     * @param errorMessage the error message from the failed attempt
     * @return the number of rows affected
     */
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.retryCount = o.retryCount + 1, o.lastError = :errorMessage WHERE o.id = :id")
    int incrementRetryCount(@Param("id") UUID id, @Param("errorMessage") String errorMessage);

    /**
     * Deletes all published events older than the specified cutoff date.
     * Used for cleanup of old, successfully processed events.
     *
     * @param cutoffDate the date before which published events should be deleted
     * @return the number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.publishedAt IS NOT NULL AND o.publishedAt < :cutoffDate")
    int deletePublishedEventsOlderThan(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Counts failed events (exceeded max retries) older than the given cutoff.
     * OUTBOX-001: failed events are never deleted — they are archived in place
     * and this count drives the cleanup alert so an event cannot disappear silently.
     *
     * @param maxRetries the maximum number of retry attempts
     * @param cutoffDate the date before which failed events should be counted
     * @return the number of archived failed events
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.publishedAt IS NULL AND o.retryCount >= :maxRetries AND o.createdAt < :cutoffDate")
    long countFailedEventsOlderThan(@Param("maxRetries") int maxRetries, @Param("cutoffDate") Instant cutoffDate);

    /**
     * Finds an event by ID with pessimistic locking.
     *
     * @param id the event ID
     * @return an Optional containing the locked event, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OutboxEvent> findWithLockById(UUID id);

    /**
     * Finds events created after a specific timestamp.
     * Useful for recovery scenarios and auditing.
     *
     * @param since the timestamp to search from
     * @param pageable pagination information
     * @return a page of events created after the specified timestamp
     */
    Page<OutboxEvent> findByCreatedAtAfterOrderBySequenceNumAsc(Instant since, Pageable pageable);
}
