package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VirtualAccountPersistencePort {
    VirtualAccountEntity save(VirtualAccountEntity virtualAccount);
    Optional<VirtualAccountEntity> findById(UUID id);
    Optional<VirtualAccountEntity> findByVaNumber(String vaNumber);
    List<VirtualAccountEntity> findExpiredPendingVAs(Instant now);
    List<VirtualAccountEntity> saveAll(Iterable<VirtualAccountEntity> virtualAccounts);
    boolean existsByVaNumber(String vaNumber);

    /**
     * IMP-2: atomic PENDING → PAID transition. Returns 1 when this caller won
     * the transition, 0 when the VA is already PAID/EXPIRED (double callback
     * or expiry raced us) — side effects must run only for the winner.
     */
    int markPaidIfPending(String vaNumber, java.math.BigDecimal paidAmount,
                          String paymentReference, Instant paidAt);

    /**
     * IMP-2: atomic PENDING → EXPIRED transition (expiry scheduler).
     */
    int markExpiredIfPending(UUID id);
}
