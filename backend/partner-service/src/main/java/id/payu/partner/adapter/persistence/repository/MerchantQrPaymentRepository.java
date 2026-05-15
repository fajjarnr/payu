package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.MerchantQrPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import id.payu.partner.domain.QrPaymentStatus;

@Repository
public interface MerchantQrPaymentRepository extends JpaRepository<MerchantQrPaymentEntity, Long> {

    Optional<MerchantQrPaymentEntity> findByReferenceId(String referenceId);

    List<MerchantQrPaymentEntity> findByMerchantIdAndStatus(Long merchantId, QrPaymentStatus status);

    @Query("SELECT qr FROM MerchantQrPaymentEntity qr WHERE qr.status = 'PENDING' AND qr.expiresAt < :now")
    List<MerchantQrPaymentEntity> findExpiredPendingPayments(@Param("now") LocalDateTime now);
}
