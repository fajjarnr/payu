package id.payu.partner.application.service;

import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.adapter.persistence.repository.PartnerCertificateRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CertificateRotationService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateRotationService.class);
    private static final int DEFAULT_ROTATION_DAYS = 90;

    @Autowired
    CertificateService certificateService;

    @Autowired
    PartnerCertificateRepository certificateRepository;

    @Transactional
    public void rotateCertificate(Long certificateId, int newValidityDays) {
        PartnerCertificate oldCert = certificateRepository.findById(certificateId).orElse(null);
        if (oldCert == null) {
            throw new IllegalArgumentException("Certificate not found with id: " + certificateId);
        }

        Long partnerId = oldCert.getPartner().getId();

        try {
            PartnerCertificate newCert = certificateService.generateKeyPairAndStore(partnerId, newValidityDays);

            oldCert.setActive(false);
            certificateRepository.save(oldCert);

            LOG.info("Successfully rotated certificate for partner {}. Old cert ID: {}, New cert ID: {}",
                    partnerId, certificateId, newCert.getId());
        } catch (Exception e) {
            LOG.error("Failed to rotate certificate for partner " + partnerId, e);
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
                rotateCertificate(cert.getId());
                rotatedCount++;
            } catch (Exception e) {
                LOG.error("Failed to rotate expiring certificate ID: " + cert.getId(), e);
            }
        }

        LOG.info("Rotated {} out of {} expiring certificates", rotatedCount, expiringCerts.size());
        return rotatedCount;
    }

    @Transactional
    public int rotateAllExpiredCertificates() {
        List<PartnerCertificate> expiredCerts = certificateRepository.findExpiredCertificates();

        int rotatedCount = 0;
        for (PartnerCertificate cert : expiredCerts) {
            try {
                rotateCertificate(cert.getId());
                rotatedCount++;
            } catch (Exception e) {
                LOG.error("Failed to rotate expired certificate ID: " + cert.getId(), e);
            }
        }

        LOG.info("Rotated {} out of {} expired certificates", rotatedCount, expiredCerts.size());
        return rotatedCount;
    }

    @Transactional
    public void rotateCertificateForPartner(Long partnerId, int newValidityDays) {
        List<PartnerCertificate> certs = certificateRepository.findByPartnerId(partnerId);

        PartnerCertificate activeCert = null;
        for (PartnerCertificate cert : certs) {
            if (cert.isActive()) {
                activeCert = cert;
                break;
            }
        }

        if (activeCert == null) {
            try {
                certificateService.generateKeyPairAndStore(partnerId, newValidityDays);
                LOG.info("Generated new certificate for partner {} (no active cert found)", partnerId);
            } catch (Exception e) {
                LOG.error("Failed to generate certificate for partner " + partnerId, e);
                throw new RuntimeException("Certificate generation failed", e);
            }
        } else {
            rotateCertificate(activeCert.getId(), newValidityDays);
        }
    }

    public boolean shouldRotateCertificate(PartnerCertificate cert, int rotationThresholdDays) {
        if (!cert.isActive()) {
            return false;
        }

        if (cert.isExpired()) {
            return true;
        }

        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().plusDays(rotationThresholdDays);
        return cert.getValidTo().isBefore(threshold);
    }
}
