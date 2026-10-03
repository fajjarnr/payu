package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.PartnerCertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerCertificateRepository extends JpaRepository<PartnerCertificateEntity, Long> {

    List<PartnerCertificateEntity> findByPartnerId(Long partnerId);

    @Query("SELECT pc FROM PartnerCertificateEntity pc WHERE pc.partner.id = ?1 AND pc.active = true")
    Optional<PartnerCertificateEntity> findActiveByPartnerId(Long partnerId);

    @Query("SELECT pc FROM PartnerCertificateEntity pc WHERE pc.partner.id = ?1 AND pc.active = true AND pc.validFrom <= ?2 AND pc.validTo >= ?3")
    Optional<PartnerCertificateEntity> findValidByPartnerId(Long partnerId, LocalDateTime validFrom, LocalDateTime validTo);

    default Optional<PartnerCertificateEntity> findValidByPartnerId(Long partnerId) {
        LocalDateTime now = LocalDateTime.now();
        return findValidByPartnerId(partnerId, now, now);
    }
    
    @Query("SELECT pc FROM PartnerCertificateEntity pc WHERE pc.partner.id = ?1 AND pc.active = true AND pc.validTo >= ?2 AND pc.validTo <= ?3")
    List<PartnerCertificateEntity> findExpiringSoon(Long partnerId, LocalDateTime now, LocalDateTime expiryThreshold);

    // findExpiringSoon(null, days) fetches any expiring cert across all partners;
    // non-null partnerId scopes to that partner only.
    
    @Query("SELECT pc FROM PartnerCertificateEntity pc WHERE pc.active = true AND pc.validTo >= ?1 AND pc.validTo <= ?2")
    List<PartnerCertificateEntity> findAllExpiringSoon(LocalDateTime now, LocalDateTime expiryThreshold);

    default List<PartnerCertificateEntity> findExpiringSoon(Long partnerId, int daysUntilExpiry) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryThreshold = now.plusDays(daysUntilExpiry);
        if (partnerId != null) {
            return findExpiringSoon(partnerId, now, expiryThreshold);
        } else {
            return findAllExpiringSoon(now, expiryThreshold);
        }
    }

    @Query("SELECT pc FROM PartnerCertificateEntity pc WHERE pc.active = true AND pc.validTo < ?1")
    List<PartnerCertificateEntity> findExpiredCertificates(LocalDateTime now);
    
    default List<PartnerCertificateEntity> findExpiredCertificates() {
        return findExpiredCertificates(LocalDateTime.now());
    }

    @Modifying
    @Query("UPDATE PartnerCertificateEntity pc SET pc.active = false WHERE pc.partner.id = ?1")
    void deactivateByPartnerId(Long partnerId);
}
