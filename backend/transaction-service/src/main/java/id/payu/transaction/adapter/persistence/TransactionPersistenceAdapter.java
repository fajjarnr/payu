package id.payu.transaction.adapter.persistence;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.adapter.persistence.repository.TransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionPersistencePort {

    private final TransactionJpaRepository transactionJpaRepository;

    @Override
    public Transaction save(Transaction transaction) {
        return transactionJpaRepository.save(transaction);
    }

    @Override
    public Optional<Transaction> findById(UUID transactionId) {
        return transactionJpaRepository.findById(transactionId);
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId, int page, int size) {
        return transactionJpaRepository.findByAccountId(accountId, PageRequest.of(page, size));
    }

    @Override
    public List<Transaction> findByReferenceNumber(String referenceNumber) {
        return transactionJpaRepository.findByReferenceNumber(referenceNumber).stream().toList();
    }
}
