package id.payu.transaction.domain.port.out;

import id.payu.transaction.domain.model.BatchDisbursement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchDisbursementRepositoryPort {
    BatchDisbursement save(BatchDisbursement batch);
    Optional<BatchDisbursement> findById(UUID id);
    Optional<BatchDisbursement> findByIdempotencyKey(String idempotencyKey);
    List<BatchDisbursement> findBySourceAccountId(UUID sourceAccountId, int limit, int offset);
    List<BatchDisbursement> findByStatus(String status, int limit);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
