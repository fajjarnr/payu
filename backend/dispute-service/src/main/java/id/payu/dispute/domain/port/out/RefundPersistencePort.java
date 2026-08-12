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

    /**
     * DISPUTE-001: serialize refund creation per transaction. Transaction-scoped
     * PostgreSQL advisory lock — works even when no refund rows exist yet, so
     * concurrent sum-then-check cannot over-refund.
     */
    void lockTransaction(UUID transactionId);
}
