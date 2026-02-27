package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.PaymentLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {

    Optional<PaymentLink> findBySlug(String slug);

    Page<PaymentLink> findByPartnerId(Long partnerId, Pageable pageable);

    @Query("SELECT pl FROM PaymentLink pl WHERE pl.status = 'ACTIVE' AND pl.expiresAt < :now")
    List<PaymentLink> findExpiredActiveLinks(@Param("now") LocalDateTime now);

    boolean existsByPartnerIdAndExternalId(Long partnerId, String externalId);

    Optional<PaymentLink> findByPartnerIdAndExternalId(Long partnerId, String externalId);

    long countByPartnerIdAndStatus(Long partnerId, PaymentLink.PaymentLinkStatus status);
}
