package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.SnapBiPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    List<SnapBiPaymentEntity> findByPartnerId(String partnerId);
}
