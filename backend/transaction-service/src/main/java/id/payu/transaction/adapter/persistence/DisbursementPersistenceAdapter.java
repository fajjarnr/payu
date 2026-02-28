package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.repository.DisbursementJpaRepository;
import id.payu.transaction.domain.model.Disbursement;
import id.payu.transaction.domain.port.out.DisbursementRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter for Disbursement aggregate.
 *
 * <p>Implements the output port using JPA repository.
 */
@Component
@RequiredArgsConstructor
public class DisbursementPersistenceAdapter implements DisbursementRepositoryPort {

    private final DisbursementJpaRepository jpaRepository;

    @Override
    public Disbursement save(Disbursement disbursement) {
        return jpaRepository.save(disbursement);
    }

    @Override
    public Optional<Disbursement> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Disbursement> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<Disbursement> findBySourceAccountId(UUID sourceAccountId, int limit, int offset) {
        return jpaRepository.findBySourceAccountId(
                sourceAccountId,
                PageRequest.of(offset / limit, limit)
        );
    }

    @Override
    public List<Disbursement> findByStatus(String status, int limit) {
        return jpaRepository.findByStatus(status, PageRequest.of(0, limit));
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.existsByIdempotencyKey(idempotencyKey);
    }
}
