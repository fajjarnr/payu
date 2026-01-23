package id.payu.partner.repository;

import id.payu.partner.domain.PartnerCertificate;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public final class PartnerCertificateRepository
        implements PanacheRepository<PartnerCertificate> {

    public List<PartnerCertificate> findByPartnerId(final Long partnerId) {
        return list("partner.id", partnerId);
    }

    public Optional<PartnerCertificate> findActiveByPartnerId(final Long partnerId) {
        return find("partner.id = ?1 and active = true", partnerId)
                .firstResultOptional();
    }

    public Optional<PartnerCertificate> findValidByPartnerId(final Long partnerId) {
        final LocalDateTime now = LocalDateTime.now();
        return find(
                "partner.id = ?1 and active = true "
                        + "and validFrom <= ?2 and validTo >= ?3",
                partnerId, now, now).firstResultOptional();
    }

    public List<PartnerCertificate> findExpiringSoon(
            final Long partnerId,
            final int daysUntilExpiry) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime expiryThreshold = now.plusDays(daysUntilExpiry);
        return list(
                "partner.id = ?1 and active = true "
                        + "and validTo >= ?2 and validTo <= ?3",
                partnerId, now, expiryThreshold);
    }

    public List<PartnerCertificate> findExpiredCertificates() {
        return list("active = true and validTo < ?1", LocalDateTime.now());
    }

    public void deactivateByPartnerId(final Long partnerId) {
        update("active = false where partner.id = ?1", partnerId);
    }
}
