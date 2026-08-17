package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionPersistencePort {
    TransactionEntity save(TransactionEntity transaction);
    Optional<TransactionEntity> findById(UUID transactionId);
    List<TransactionEntity> findByAccountId(UUID accountId, int page, int size);
    default List<TransactionEntity> findByAccountIdKeyset(UUID accountId, Instant lastCreatedAt, UUID lastId, int limit) {
        return findByAccountId(accountId, 0, limit);
    }
    long countByAccountId(UUID accountId);
    List<TransactionEntity> findByReferenceNumber(String referenceNumber);

    /**
     * IMP-5: row locked FOR UPDATE — serializes concurrent callbacks so the
     * terminal-status check in the settle flow is race-free.
     */
    List<TransactionEntity> findByReferenceNumberForUpdate(String referenceNumber);
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
    List<TransactionEntity> findExpiredPendingTransactions(Instant now);
    List<TransactionEntity> saveAll(Iterable<TransactionEntity> transactions);
}
