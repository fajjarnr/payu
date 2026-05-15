package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;

/**
 * JPA repository for TransactionEntity entity with sharding/partitioning support.
 *
 * <p>When sharding is enabled, PostgreSQL automatically handles partition routing
 * through partition pruning. The queries below work transparently with both
 * the legacy transactions table and the new transactions_partitioned table.</p>
 *
 * <p>Key partition-aware queries:</p>
 * <ul>
 *   <li>findBySenderAccountId - Uses partition pruning (single partition)</li>
 *   <li>findByRecipientAccountId - Cross-partition scan (all partitions)</li>
 *   <li>findByAccountId - Combines sender and recipient lookups</li>
 * </ul>
 */
@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

    /**
     * Find transaction by unique reference number.
     * Global index ensures efficient lookup across all partitions.
     */
    Optional<TransactionEntity> findByReferenceNumber(String referenceNumber);
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find transactions for an account (both sender and recipient).
     * When sharding is enabled, this queries both:
     * - Sender partition (uses partition pruning)
     * - All partitions for recipient lookups
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.senderAccountId = :accountId OR t.recipientAccountId = :accountId ORDER BY t.createdAt DESC")
    List<TransactionEntity> findByAccountId(@Param("accountId") UUID accountId,
                                       org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.senderAccountId = :accountId OR t.recipientAccountId = :accountId")
    long countByAccountId(@Param("accountId") UUID accountId);

    /**
     * Find transactions by sender account ID only.
     * When sharding is enabled, PostgreSQL prunes to a single partition.
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.senderAccountId = :accountId ORDER BY t.createdAt DESC")
    List<TransactionEntity> findBySenderAccountId(@Param("accountId") UUID accountId,
                                             org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions by recipient account ID only.
     * When sharding is enabled, this requires scanning all partitions.
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.recipientAccountId = :accountId ORDER BY t.createdAt DESC")
    List<TransactionEntity> findByRecipientAccountId(@Param("accountId") UUID accountId,
                                                org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions by status with pagination.
     * Useful for operational queries and monitoring.
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<TransactionEntity> findByStatus(@Param("status") TransactionStatus status,
                                     org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions by type with pagination.
     * Useful for analytics and reporting.
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.type = :type ORDER BY t.createdAt DESC")
    List<TransactionEntity> findByType(@Param("type") TransactionType type,
                                  org.springframework.data.domain.Pageable pageable);

    /**
     * Count transactions by sender account ID.
     * Uses partition pruning when sharding is enabled.
     */
    long countBySenderAccountId(UUID senderAccountId);

    /**
     * Count transactions by recipient account ID.
     * Requires scanning all partitions when sharding is enabled.
     */
    long countByRecipientAccountId(UUID recipientAccountId);

    /**
     * Find pending/processing transactions that have expired.
     * Used by PaymentExpiryScheduler to auto-cancel expired payments.
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.status IN ('PENDING', 'PROCESSING') " +
           "AND t.expiresAt IS NOT NULL AND t.expiresAt < :now")
    List<TransactionEntity> findExpiredPendingTransactions(@Param("now") java.time.Instant now);
}
