package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionPersistencePort {
    TransactionEntity save(TransactionEntity transaction);
    Optional<TransactionEntity> findById(UUID transactionId);
    List<TransactionEntity> findByAccountId(UUID accountId, int page, int size);
    long countByAccountId(UUID accountId);
    List<TransactionEntity> findByReferenceNumber(String referenceNumber);
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
}
