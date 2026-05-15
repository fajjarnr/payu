package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.PaymentLinkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import id.payu.partner.domain.PaymentLinkStatus;

@Repository
public interface PaymentLinkRepository extends JpaRepository<PaymentLinkEntity, Long> {

    Optional<PaymentLinkEntity> findBySlug(String slug);

    Page<PaymentLinkEntity> findByPartnerId(Long partnerId, Pageable pageable);

    @Query("SELECT pl FROM PaymentLinkEntity pl WHERE pl.status = 'ACTIVE' AND pl.expiresAt < :now")
    List<PaymentLinkEntity> findExpiredActiveLinks(@Param("now") LocalDateTime now);

    boolean existsByPartnerIdAndExternalId(Long partnerId, String externalId);

    Optional<PaymentLinkEntity> findByPartnerIdAndExternalId(Long partnerId, String externalId);

    long countByPartnerIdAndStatus(Long partnerId, PaymentLinkStatus status);
}
