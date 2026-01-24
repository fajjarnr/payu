package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Transaction entity with sharding/partitioning support.
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
public interface TransactionJpaRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find transaction by unique reference number.
     * Global index ensures efficient lookup across all partitions.
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /**
     * Find transactions for an account (both sender and recipient).
     * When sharding is enabled, this queries both:
     * - Sender partition (uses partition pruning)
     * - All partitions for recipient lookups
     */
    @Query("SELECT t FROM Transaction t WHERE t.senderAccountId = :accountId OR t.recipientAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountId(@Param("accountId") UUID accountId,
                                       org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions by sender account ID only.
     * When sharding is enabled, PostgreSQL prunes to a single partition.
     */
    @Query("SELECT t FROM Transaction t WHERE t.senderAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findBySenderAccountId(@Param("accountId") UUID accountId,
                                             org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions by recipient account ID only.
     * When sharding is enabled, this requires scanning all partitions.
     */
    @Query("SELECT t FROM Transaction t WHERE t.recipientAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findByRecipientAccountId(@Param("accountId") UUID accountId,
                                                org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions by status with pagination.
     * Useful for operational queries and monitoring.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByStatus(@Param("status") Transaction.TransactionStatus status,
                                     org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions by type with pagination.
     * Useful for analytics and reporting.
     */
    @Query("SELECT t FROM Transaction t WHERE t.type = :type ORDER BY t.createdAt DESC")
    List<Transaction> findByType(@Param("type") Transaction.TransactionType type,
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
}
