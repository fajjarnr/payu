package id.payu.saga.repository;

import id.payu.saga.entity.SagaInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SagaInstance persistence.
 * Provides CRUD operations and custom queries for saga management.
 */
@Repository
public interface SagaRepository extends JpaRepository<SagaInstance, String> {

    /**
     * Find saga by ID with optimistic locking.
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.sagaId = :sagaId")
    Optional<SagaInstance> findBySagaId(@Param("sagaId") String sagaId);

    /**
     * Find all sagas by type.
     */
    List<SagaInstance> findBySagaType(String sagaType);

    /**
     * Find all sagas by current state.
     */
    List<SagaInstance> findByCurrentState(String currentState);

    /**
     * Find sagas by type and state.
     */
    List<SagaInstance> findBySagaTypeAndCurrentState(String sagaType, String currentState);

    /**
     * Find pending sagas that have not been updated for a given duration.
     * Useful for detecting stalled sagas.
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.currentState NOT IN ('COMPLETED', 'FAILED', 'COMPENSATED') " +
           "AND s.lastUpdatedAt < :threshold")
    List<SagaInstance> findStalledSagas(@Param("threshold") Instant threshold);

    /**
     * Find sagas that need retry (failed but not exceeded max retries).
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.currentState = 'FAILED' " +
           "AND s.retryCount < s.maxRetries AND s.lastUpdatedAt < :retryThreshold")
    List<SagaInstance> findRetryableSagas(@Param("retryThreshold") Instant retryThreshold);

    /**
     * Count sagas by type and state.
     */
    long countBySagaTypeAndCurrentState(String sagaType, String currentState);

    /**
     * Check if saga exists by ID.
     */
    boolean existsBySagaId(String sagaId);

    /**
     * Update saga state with optimistic locking check.
     */
    @Modifying
    @Query("UPDATE SagaInstance s SET s.currentState = :newState, s.previousState = s.currentState, " +
           "s.lastUpdatedAt = :now, s.version = s.version + 1 " +
           "WHERE s.sagaId = :sagaId AND s.version = :expectedVersion")
    int updateStateWithVersion(@Param("sagaId") String sagaId,
                               @Param("newState") String newState,
                               @Param("expectedVersion") Long expectedVersion,
                               @Param("now") Instant now);

    /**
     * Find sagas started within a time range.
     */
    List<SagaInstance> findByStartedAtBetween(Instant start, Instant end);

    /**
     * Find sagas by type with pagination.
     */
    Page<SagaInstance> findBySagaType(String sagaType, Pageable pageable);

    /**
     * Find incomplete sagas (not in terminal states).
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.currentState NOT IN ('COMPLETED', 'FAILED', 'COMPENSATED')")
    List<SagaInstance> findIncompleteSagas();

    /**
     * Delete completed sagas older than a given date.
     */
    @Modifying
    @Query("DELETE FROM SagaInstance s WHERE s.currentState = 'COMPLETED' AND s.completedAt < :threshold")
    int deleteOldCompletedSagas(@Param("threshold") Instant threshold);

    /**
     * Search sagas by payload content using PostgreSQL JSONB operator.
     */
    @Query(value = "SELECT * FROM saga_instances s WHERE s.payload @> :jsonQuery::jsonb", nativeQuery = true)
    List<SagaInstance> findByPayloadContaining(@Param("jsonQuery") String jsonQuery);

    /**
     * Find sagas by correlation ID stored in payload.
     */
    @Query(value = "SELECT * FROM saga_instances s WHERE s.payload ->> 'correlationId' = :correlationId", nativeQuery = true)
    List<SagaInstance> findByCorrelationId(@Param("correlationId") String correlationId);
}
