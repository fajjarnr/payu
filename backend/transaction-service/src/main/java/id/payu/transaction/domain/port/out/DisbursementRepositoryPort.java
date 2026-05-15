package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisbursementRepositoryPort {
    DisbursementEntity save(DisbursementEntity disbursement);
    Optional<DisbursementEntity> findById(UUID id);
    Optional<DisbursementEntity> findByIdempotencyKey(String idempotencyKey);
    List<DisbursementEntity> findBySourceAccountId(UUID sourceAccountId, int limit, int offset);
    List<DisbursementEntity> findByStatus(String status, int limit);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
