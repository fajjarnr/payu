package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.VaPaymentRecordEntity;
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
     * ARCH-TXN-001: locks the VA row (PESSIMISTIC_WRITE) for the PENDING → PAID
     * transition — of two concurrent callbacks exactly one wins; the loser
     * observes status PAID and must be a no-op.
     */
    Optional<VirtualAccountEntity> findWithLockByVaNumber(String vaNumber);

    /**
     * ARCH-TXN-001: appends an immutable payment record (va_payment_records).
     * Insert-only; never updated or deleted.
     */
    VaPaymentRecordEntity savePaymentRecord(VaPaymentRecordEntity record);

    /**
     * IMP-2: atomic PENDING → EXPIRED transition (expiry scheduler).
     */
    int markExpiredIfPending(UUID id);
}
