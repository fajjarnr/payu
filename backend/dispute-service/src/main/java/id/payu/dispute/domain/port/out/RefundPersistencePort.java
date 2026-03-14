package id.payu.dispute.domain.port.out;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.RefundStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundPersistencePort {
    Refund save(Refund refund);
    Optional<Refund> findById(UUID id);
    List<Refund> findByTransactionId(UUID transactionId);
    List<Refund> findByStatus(RefundStatus status);
    List<Refund> findAll();
    void deleteById(UUID id);
}
