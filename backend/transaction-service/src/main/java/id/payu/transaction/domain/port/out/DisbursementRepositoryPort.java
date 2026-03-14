package id.payu.transaction.domain.port.out;

import id.payu.transaction.domain.model.Disbursement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisbursementRepositoryPort {
    Disbursement save(Disbursement disbursement);
    Optional<Disbursement> findById(UUID id);
    Optional<Disbursement> findByIdempotencyKey(String idempotencyKey);
    List<Disbursement> findBySourceAccountId(UUID sourceAccountId, int limit, int offset);
    List<Disbursement> findByStatus(String status, int limit);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
