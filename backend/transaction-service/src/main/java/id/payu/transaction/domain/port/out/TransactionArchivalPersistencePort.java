package id.payu.transaction.domain.port.out;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.model.TransactionArchive;

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
    List<Transaction> findTransactionsToArchive(Instant cutoffDate, int batchSize);

    /**
     * Archive transactions by saving them to archive table
     */
    void archiveTransactions(List<TransactionArchive> archives);

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
    List<TransactionArchive> findByAccountId(UUID accountId, int page, int size);

    /**
     * Find archived transactions by batch ID
     */
    List<TransactionArchive> findByBatchId(Long batchId);
}
