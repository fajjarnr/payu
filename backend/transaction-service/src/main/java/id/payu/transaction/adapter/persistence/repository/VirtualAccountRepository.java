package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;
import id.payu.transaction.domain.model.VaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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

    boolean existsByVaNumber(String vaNumber);
}
