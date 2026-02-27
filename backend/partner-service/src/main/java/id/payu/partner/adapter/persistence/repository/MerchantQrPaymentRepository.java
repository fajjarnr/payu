package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.MerchantQrPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantQrPaymentRepository extends JpaRepository<MerchantQrPayment, Long> {

    Optional<MerchantQrPayment> findByReferenceId(String referenceId);

    List<MerchantQrPayment> findByMerchantIdAndStatus(Long merchantId, MerchantQrPayment.QrPaymentStatus status);

    @Query("SELECT qr FROM MerchantQrPayment qr WHERE qr.status = 'PENDING' AND qr.expiresAt < :now")
    List<MerchantQrPayment> findExpiredPendingPayments(@Param("now") LocalDateTime now);
}
