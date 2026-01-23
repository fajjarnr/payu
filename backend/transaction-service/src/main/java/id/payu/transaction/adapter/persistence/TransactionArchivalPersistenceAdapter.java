package id.payu.transaction.adapter.persistence;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.model.TransactionArchive;
import id.payu.transaction.domain.port.out.TransactionArchivalPersistencePort;
import id.payu.transaction.adapter.persistence.repository.TransactionArchiveJpaRepository;
import id.payu.transaction.adapter.persistence.repository.TransactionJpaRepository;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionArchivalPersistenceAdapter implements TransactionArchivalPersistencePort {

    private final TransactionArchiveJpaRepository transactionArchiveJpaRepository;
    private final TransactionJpaRepository transactionJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Transaction> findTransactionsToArchive(Instant beforeDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transactionArchiveJpaRepository.findTransactionsToArchive(beforeDate, pageable);
    }

    @Override
    @Transactional
    public void archiveTransactions(List<TransactionArchive> archives) {
        if (archives.isEmpty()) {
            return;
        }
        transactionArchiveJpaRepository.saveAll(archives);
    }

    @Override
    @Transactional
    public void deleteArchivedTransactions(List<UUID> transactionIds) {
        if (transactionIds.isEmpty()) {
            return;
        }

        String sql = "DELETE FROM transactions WHERE id = ANY(?)";
        jdbcTemplate.update(sql, transactionIds.toArray());
    }

    @Override
    public Long getNextBatchId() {
        String sql = "SELECT nextval('archival_batch_id_seq')";
        Long batchId = jdbcTemplate.queryForObject(sql, Long.class);
        return batchId != null ? batchId : 1L;
    }

    @Override
    public long countTransactionsToArchive(Instant beforeDate) {
        return transactionArchiveJpaRepository.countTransactionsToArchive(beforeDate);
    }

    @Override
    public List<TransactionArchive> findByAccountId(UUID accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionArchiveJpaRepository.findByAccountId(accountId, pageable);
    }

    @Override
    public List<TransactionArchive> findByBatchId(Long batchId) {
        return transactionArchiveJpaRepository.findByArchivedBatchId(batchId);
    }
}
