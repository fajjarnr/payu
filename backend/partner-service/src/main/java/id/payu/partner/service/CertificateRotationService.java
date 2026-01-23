package id.payu.partner.service;

import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.repository.PartnerCertificateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.security.*;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CertificateRotationService {

    private static final Logger LOG = Logger.getLogger(CertificateRotationService.class.getName());
    private static final int DEFAULT_ROTATION_DAYS = 90;

    @Inject
    CertificateService certificateService;

    @Inject
    PartnerCertificateRepository certificateRepository;

    @Transactional
    public void rotateCertificate(Long certificateId, int newValidityDays) {
        PartnerCertificate oldCert = certificateRepository.findById(certificateId);
        if (oldCert == null) {
            throw new IllegalArgumentException("Certificate not found with id: " + certificateId);
        }

        Long partnerId = oldCert.partner.id;

        try {
            PartnerCertificate newCert = certificateService.generateKeyPairAndStore(partnerId, newValidityDays);

            oldCert.active = false;

            LOG.log(Level.INFO, "Successfully rotated certificate for partner {0}. Old cert ID: {1}, New cert ID: {2}",
                    new Object[]{partnerId, certificateId, newCert.id});
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to rotate certificate for partner " + partnerId, e);
            throw new RuntimeException("Certificate rotation failed", e);
        }
    }

    @Transactional
    public void rotateCertificate(Long certificateId) {
        rotateCertificate(certificateId, DEFAULT_ROTATION_DAYS);
    }

    @Transactional
    public int rotateExpiringCertificates(int daysUntilExpiry) {
        List<PartnerCertificate> expiringCerts = certificateRepository.findExpiringSoon(null, daysUntilExpiry);

        int rotatedCount = 0;
        for (PartnerCertificate cert : expiringCerts) {
            try {
                rotateCertificate(cert.id);
                rotatedCount++;
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to rotate expiring certificate ID: " + cert.id, e);
            }
        }

        LOG.log(Level.INFO, "Rotated {0} out of {1} expiring certificates", new Object[]{rotatedCount, expiringCerts.size()});
        return rotatedCount;
    }

    @Transactional
    public int rotateAllExpiredCertificates() {
        List<PartnerCertificate> expiredCerts = certificateRepository.findExpiredCertificates();

        int rotatedCount = 0;
        for (PartnerCertificate cert : expiredCerts) {
            try {
                rotateCertificate(cert.id);
                rotatedCount++;
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to rotate expired certificate ID: " + cert.id, e);
            }
        }

        LOG.log(Level.INFO, "Rotated {0} out of {1} expired certificates", new Object[]{rotatedCount, expiredCerts.size()});
        return rotatedCount;
    }

    @Transactional
    public void rotateCertificateForPartner(Long partnerId, int newValidityDays) {
        List<PartnerCertificate> certs = certificateRepository.findByPartnerId(partnerId);

        PartnerCertificate activeCert = null;
        for (PartnerCertificate cert : certs) {
            if (cert.active) {
                activeCert = cert;
                break;
            }
        }

        if (activeCert == null) {
            try {
                certificateService.generateKeyPairAndStore(partnerId, newValidityDays);
                LOG.log(Level.INFO, "Generated new certificate for partner {0} (no active cert found)", partnerId);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to generate certificate for partner " + partnerId, e);
                throw new RuntimeException("Certificate generation failed", e);
            }
        } else {
            rotateCertificate(activeCert.id, newValidityDays);
        }
    }

    public boolean shouldRotateCertificate(PartnerCertificate cert, int rotationThresholdDays) {
        if (!cert.active) {
            return false;
        }

        if (cert.isExpired()) {
            return true;
        }

        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().plusDays(rotationThresholdDays);
        return cert.validTo.isBefore(threshold);
    }
}
