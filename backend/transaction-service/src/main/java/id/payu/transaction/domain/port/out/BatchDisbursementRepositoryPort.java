package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.BatchDisbursementEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchDisbursementRepositoryPort {
    BatchDisbursementEntity save(BatchDisbursementEntity batch);
    Optional<BatchDisbursementEntity> findById(UUID id);
    Optional<BatchDisbursementEntity> findByIdempotencyKey(String idempotencyKey);
    List<BatchDisbursementEntity> findBySourceAccountId(UUID sourceAccountId, int limit, int offset);
    List<BatchDisbursementEntity> findByStatus(String status, int limit);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
