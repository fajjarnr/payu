package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.PartnerCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerCertificateRepository extends JpaRepository<PartnerCertificate, Long> {

    List<PartnerCertificate> findByPartnerId(Long partnerId);

    @Query("SELECT pc FROM PartnerCertificate pc WHERE pc.partner.id = ?1 AND pc.active = true")
    Optional<PartnerCertificate> findActiveByPartnerId(Long partnerId);

    @Query("SELECT pc FROM PartnerCertificate pc WHERE pc.partner.id = ?1 AND pc.active = true AND pc.validFrom <= ?2 AND pc.validTo >= ?3")
    Optional<PartnerCertificate> findValidByPartnerId(Long partnerId, LocalDateTime validFrom, LocalDateTime validTo);

    default Optional<PartnerCertificate> findValidByPartnerId(Long partnerId) {
        LocalDateTime now = LocalDateTime.now();
        return findValidByPartnerId(partnerId, now, now);
    }
    
    @Query("SELECT pc FROM PartnerCertificate pc WHERE pc.partner.id = ?1 AND pc.active = true AND pc.validTo >= ?2 AND pc.validTo <= ?3")
    List<PartnerCertificate> findExpiringSoon(Long partnerId, LocalDateTime now, LocalDateTime expiryThreshold);

    // Helper method to satisfy the logical contract - if partnerId is null, ignore it? Or specific call?
    // Based on usage in RotationService: findExpiringSoon(null, days) suggests fetching ANY expiring cert.
    // However, the original code had "partner.id = ?1" which implies partnerId MUST be provided or it fails/returns empty for null partnerId.
    // Let's check original usage. rotateExpiringCertificates passes null partnerId.
    // But original query: "partner.id = ?1 ..."
    // If param 1 is null, it matches where partner_id is null? But partner_id is likely FK not null.
    // The original Panache code might have been handling null dynamically or it was a bug?
    // "partner.id = ?1" in HQL usually means exact match.
    
    // I will create a separate method for "Any partner" expiring check if needed.
    // "request param partnerId" in controller suggests explicit partner.
    // "rotateExpiringCertificates" service job suggests ALL partners.
    
    @Query("SELECT pc FROM PartnerCertificate pc WHERE pc.active = true AND pc.validTo >= ?1 AND pc.validTo <= ?2")
    List<PartnerCertificate> findAllExpiringSoon(LocalDateTime now, LocalDateTime expiryThreshold);

    default List<PartnerCertificate> findExpiringSoon(Long partnerId, int daysUntilExpiry) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryThreshold = now.plusDays(daysUntilExpiry);
        if (partnerId != null) {
            return findExpiringSoon(partnerId, now, expiryThreshold);
        } else {
            return findAllExpiringSoon(now, expiryThreshold);
        }
    }

    @Query("SELECT pc FROM PartnerCertificate pc WHERE pc.active = true AND pc.validTo < ?1")
    List<PartnerCertificate> findExpiredCertificates(LocalDateTime now);
    
    default List<PartnerCertificate> findExpiredCertificates() {
        return findExpiredCertificates(LocalDateTime.now());
    }

    @Modifying
    @Query("UPDATE PartnerCertificate pc SET pc.active = false WHERE pc.partner.id = ?1")
    void deactivateByPartnerId(Long partnerId);
}
