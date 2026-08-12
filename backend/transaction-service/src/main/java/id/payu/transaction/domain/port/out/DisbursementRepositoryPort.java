package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisbursementRepositoryPort {
    DisbursementEntity save(DisbursementEntity disbursement);
    DisbursementEntity persistNew(DisbursementEntity disbursement);
    Optional<DisbursementEntity> findById(UUID id);

    /**
     * IMP-5: row locked FOR UPDATE — serializes concurrent callbacks so the
     * terminal-status check in complete/fail is race-free.
     */
    Optional<DisbursementEntity> findByIdForUpdate(UUID id);
    Optional<DisbursementEntity> findByIdempotencyKey(String idempotencyKey);
    List<DisbursementEntity> findBySourceAccountId(UUID sourceAccountId, int limit, int offset);
    List<DisbursementEntity> findByStatus(String status, int limit);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
