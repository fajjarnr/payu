package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.SnapBiPaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * BUG-BE-182 FIX: JPA repository for SNAP BI payment persistence.
 */
@Repository
public interface SnapBiPaymentRepository extends JpaRepository<SnapBiPaymentEntity, Long> {

    Optional<SnapBiPaymentEntity> findByPayuReferenceNo(String payuReferenceNo);

    Optional<SnapBiPaymentEntity> findByPartnerIdAndPayuReferenceNo(String partnerId, String payuReferenceNo);

    Optional<SnapBiPaymentEntity> findByPartnerIdAndPartnerReferenceNo(String partnerId, String partnerReferenceNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SnapBiPaymentEntity p WHERE p.partnerId = :partnerId "
            + "AND (p.payuReferenceNo = :referenceNo OR p.partnerReferenceNo = :referenceNo)")
    Optional<SnapBiPaymentEntity> findForUpdateByPartnerIdAndReferenceNo(
            @Param("partnerId") String partnerId, @Param("referenceNo") String referenceNo);

    List<SnapBiPaymentEntity> findByPartnerId(String partnerId);

    // PARTNER-PROD-005: all payments created after a cutoff for reconciliation.
    List<SnapBiPaymentEntity> findByCreatedAtAfter(Instant createdAt);
}
