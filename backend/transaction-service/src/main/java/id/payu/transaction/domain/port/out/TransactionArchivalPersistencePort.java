package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.adapter.persistence.entity.TransactionArchiveEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionArchivalPersistencePort {

    /**
     * Count transactions that need to be archived
     */
    long countTransactionsToArchive(Instant cutoffDate);

    /**
     * Find transactions to archive (paginated by batch size)
     */
    List<TransactionEntity> findTransactionsToArchive(Instant cutoffDate, int batchSize);

    /**
     * Archive transactions by saving them to archive table
     */
    void archiveTransactions(List<TransactionArchiveEntity> archives);

    /**
     * Delete transactions that have been archived
     */
    void deleteArchivedTransactions(List<UUID> transactionIds);

    /**
     * Get next batch ID for archival
     */
    Long getNextBatchId();

    /**
     * Find archived transactions by account ID
     */
    List<TransactionArchiveEntity> findByAccountId(UUID accountId, int page, int size);

    /**
     * Find archived transactions by batch ID
     */
    List<TransactionArchiveEntity> findByBatchId(Long batchId);
}
