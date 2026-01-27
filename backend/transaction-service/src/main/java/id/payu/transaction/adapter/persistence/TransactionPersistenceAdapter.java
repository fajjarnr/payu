package id.payu.transaction.adapter.persistence;

import id.payu.transaction.config.ShardingConfig;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.adapter.persistence.repository.TransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionPersistenceAdapter implements TransactionPersistencePort {

    private final TransactionJpaRepository transactionJpaRepository;
    private final ShardingConfig shardingConfig;

    @Override
    @Transactional
    public Transaction save(Transaction transaction) {
        if (shardingConfig.isEnabled()) {
            int partition = shardingConfig.calculatePartition(transaction.getSenderAccountId());
            log.debug("Saving transaction {} to partition {}", transaction.getId(), partition);
        }
        return transactionJpaRepository.save(transaction);
    }

    @Override
    public Optional<Transaction> findById(UUID transactionId) {
        if (shardingConfig.isEnabled()) {
            log.debug("Finding transaction by ID {} (will scan all partitions)", transactionId);
        }
        return transactionJpaRepository.findById(transactionId);
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId, int page, int size) {
        if (shardingConfig.isEnabled()) {
            log.debug("Finding transactions for account {} (sender+recipient lookup)", accountId);
        }
        return transactionJpaRepository.findByAccountId(accountId, PageRequest.of(page, size));
    }

    @Override
    public List<Transaction> findByReferenceNumber(String referenceNumber) {
        if (shardingConfig.isEnabled()) {
            log.debug("Finding transactions by reference number {}", referenceNumber);
        }
        return transactionJpaRepository.findByReferenceNumber(referenceNumber).stream().toList();
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        if (shardingConfig.isEnabled()) {
            log.debug("Finding transactions by idempotency key {}", idempotencyKey);
        }
        return transactionJpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    /**
     * Find transactions by sender account ID (partition-aware).
     * When sharding is enabled, this query benefits from partition pruning.
     *
     * @param senderAccountId the sender account ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return list of transactions
     */
    public List<Transaction> findBySenderAccountId(UUID senderAccountId, int page, int size) {
        if (shardingConfig.isEnabled()) {
            int partition = shardingConfig.calculatePartition(senderAccountId);
            log.debug("Querying partition {} for sender account {}", partition, senderAccountId);
        }
        return transactionJpaRepository.findBySenderAccountId(senderAccountId, PageRequest.of(page, size));
    }

    /**
     * Find transactions by recipient account ID.
     * When sharding is enabled, this requires cross-partition scanning.
     *
     * @param recipientAccountId the recipient account ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return list of transactions
     */
    public List<Transaction> findByRecipientAccountId(UUID recipientAccountId, int page, int size) {
        if (shardingConfig.isEnabled()) {
            log.debug("Querying all partitions for recipient account {} (cross-partition scan)",
                    recipientAccountId);
        }
        return transactionJpaRepository.findByRecipientAccountId(recipientAccountId,
                PageRequest.of(page, size));
    }
}
