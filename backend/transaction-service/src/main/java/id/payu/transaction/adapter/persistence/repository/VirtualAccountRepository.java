package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;
import id.payu.transaction.domain.model.VaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VirtualAccountRepository extends JpaRepository<VirtualAccountEntity, UUID> {

    Optional<VirtualAccountEntity> findByVaNumber(String vaNumber);

    List<VirtualAccountEntity> findByPartnerIdAndStatus(UUID partnerId, VaStatus status);

    Optional<VirtualAccountEntity> findByPartnerIdAndExternalId(UUID partnerId, String externalId);

    @Query("SELECT va FROM VirtualAccountEntity va WHERE va.status = 'PENDING' AND va.expiresAt < :now")
    List<VirtualAccountEntity> findExpiredPendingVAs(@Param("now") Instant now);

    /**
     * ARCH-TXN-001: row-level lock for the PENDING → PAID transition. Two
     * concurrent bank callbacks serialize here; the loser sees status PAID
     * and returns the existing result without appending a second payment
     * record. Replaces the in-place @Modifying UPDATE (immutable ledger).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT va FROM VirtualAccountEntity va WHERE va.vaNumber = :vaNumber")
    Optional<VirtualAccountEntity> findWithLockByVaNumber(@Param("vaNumber") String vaNumber);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VirtualAccountEntity va SET va.status = 'EXPIRED' "
            + "WHERE va.id = :id AND va.status = 'PENDING'")
    int markExpiredIfPending(@Param("id") UUID id);

    boolean existsByVaNumber(String vaNumber);
}
