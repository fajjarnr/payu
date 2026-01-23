package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.model.TransactionArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionArchiveJpaRepository extends JpaRepository<TransactionArchive, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.status = 'COMPLETED' AND t.completedAt < :beforeDate ORDER BY t.completedAt ASC")
    List<Transaction> findTransactionsToArchive(@Param("beforeDate") Instant beforeDate, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'COMPLETED' AND t.completedAt < :beforeDate")
    long countTransactionsToArchive(@Param("beforeDate") Instant beforeDate);

    @Query("SELECT t FROM TransactionArchive t WHERE t.senderAccountId = :accountId OR t.recipientAccountId = :accountId ORDER BY t.createdAt DESC")
    List<TransactionArchive> findByAccountId(@Param("accountId") UUID accountId, org.springframework.data.domain.Pageable pageable);

    List<TransactionArchive> findByArchivedBatchId(Long archivedBatchId);
}
