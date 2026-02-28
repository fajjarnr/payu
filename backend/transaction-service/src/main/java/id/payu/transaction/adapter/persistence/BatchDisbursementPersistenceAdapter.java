package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.repository.BatchDisbursementJpaRepository;
import id.payu.transaction.domain.model.BatchDisbursement;
import id.payu.transaction.domain.port.out.BatchDisbursementRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter for BatchDisbursement aggregate.
 *
 * <p>Implements the output port using JPA repository.
 */
@Component
@RequiredArgsConstructor
public class BatchDisbursementPersistenceAdapter implements BatchDisbursementRepositoryPort {

    private final BatchDisbursementJpaRepository jpaRepository;

    @Override
    public BatchDisbursement save(BatchDisbursement batch) {
        return jpaRepository.save(batch);
    }

    @Override
    public Optional<BatchDisbursement> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<BatchDisbursement> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<BatchDisbursement> findBySourceAccountId(UUID sourceAccountId, int limit, int offset) {
        return jpaRepository.findBySourceAccountId(
                sourceAccountId,
                PageRequest.of(offset / limit, limit)
        );
    }

    @Override
    public List<BatchDisbursement> findByStatus(String status, int limit) {
        return jpaRepository.findByStatus(status, PageRequest.of(0, limit));
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.existsByIdempotencyKey(idempotencyKey);
    }
}
