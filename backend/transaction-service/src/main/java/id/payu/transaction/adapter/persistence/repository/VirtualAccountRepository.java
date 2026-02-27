package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.domain.model.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, UUID> {

    Optional<VirtualAccount> findByVaNumber(String vaNumber);

    List<VirtualAccount> findByPartnerIdAndStatus(UUID partnerId, VirtualAccount.VaStatus status);

    Optional<VirtualAccount> findByPartnerIdAndExternalId(UUID partnerId, String externalId);

    @Query("SELECT va FROM VirtualAccount va WHERE va.status = 'PENDING' AND va.expiresAt < :now")
    List<VirtualAccount> findExpiredPendingVAs(@Param("now") Instant now);

    boolean existsByVaNumber(String vaNumber);
}
