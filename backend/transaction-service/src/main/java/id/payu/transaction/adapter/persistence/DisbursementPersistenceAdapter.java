package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.repository.DisbursementJpaRepository;
import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.domain.port.out.DisbursementRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter for DisbursementEntity aggregate.
 *
 * <p>Implements the output port using JPA repository.
 */
@Component
@RequiredArgsConstructor
public class DisbursementPersistenceAdapter implements DisbursementRepositoryPort {

    private final DisbursementJpaRepository jpaRepository;

    @Override
    public DisbursementEntity save(DisbursementEntity disbursement) {
        return jpaRepository.save(disbursement);
    }

    /**
     * Persist a new disbursement bypassing Spring Data JPA's isNew() detection.
     *
     * <p>Use this for new entities where you have manually assigned the ID (e.g.,
     * for stable cross-service transaction references) but the @Version is null
     * because the entity has not been persisted yet. Spring Data JPA's default
     * detection sees {@code id != null && version == null} as "detached" and
     * calls merge() (which fails with StaleObjectStateException for new rows).</p>
     *
     * <p>Calling EntityManager.persist() directly is the correct JPA pattern for
     * new entities per context7/spring-projects/spring-data-jpa documentation.</p>
     */
    @Override
    public DisbursementEntity persistNew(DisbursementEntity disbursement) {
        if (disbursement.getId() == null) {
            throw new IllegalStateException("Cannot persistNew: id is null");
        }
        jpaRepository.persistNew(disbursement);
        return jpaRepository.findById(disbursement.getId())
                .orElseThrow(() -> new IllegalStateException("Entity not found after persist: " + disbursement.getId()));
    }

    @Override
    public Optional<DisbursementEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<DisbursementEntity> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id);
    }

    @Override
    public Optional<DisbursementEntity> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<DisbursementEntity> findBySourceAccountId(UUID sourceAccountId, int limit, int offset) {
        return jpaRepository.findBySourceAccountId(
                sourceAccountId,
                PageRequest.of(offset / limit, limit)
        );
    }

    @Override
    public List<DisbursementEntity> findByStatus(String status, int limit) {
        return jpaRepository.findByStatus(status, PageRequest.of(0, limit));
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.existsByIdempotencyKey(idempotencyKey);
    }
}
