package id.payu.transaction.domain.port.out;

import id.payu.transaction.domain.model.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionPersistencePort {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID transactionId);
    List<Transaction> findByAccountId(UUID accountId, int page, int size);
    List<Transaction> findByReferenceNumber(String referenceNumber);
}
