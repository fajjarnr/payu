package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.SnapBiPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BUG-BE-182 FIX: JPA repository for SNAP BI payment persistence.
 */
@Repository
public interface SnapBiPaymentRepository extends JpaRepository<SnapBiPayment, Long> {

    Optional<SnapBiPayment> findByPayuReferenceNo(String payuReferenceNo);

    Optional<SnapBiPayment> findByPartnerIdAndPayuReferenceNo(String partnerId, String payuReferenceNo);

    Optional<SnapBiPayment> findByPartnerIdAndPartnerReferenceNo(String partnerId, String partnerReferenceNo);

    List<SnapBiPayment> findByPartnerId(String partnerId);
}
