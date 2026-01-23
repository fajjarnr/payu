package id.payu.partner.service;

import id.payu.partner.domain.Partner;
import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.repository.PartnerCertificateRepository;
import id.payu.partner.repository.PartnerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CertificateService {

    @Inject
    PartnerCertificateRepository certificateRepository;

    @Inject
    PartnerRepository partnerRepository;

    @Inject
    SnapBiSignatureService signatureService;

    @Transactional
    public PartnerCertificate addCertificate(Long partnerId, String certificatePem, String privateKeyPem) {
        Partner partner = partnerRepository.findById(partnerId);
        if (partner == null) {
            throw new IllegalArgumentException("Partner not found with id: " + partnerId);
        }

        try {
            X509Certificate cert = parseCertificate(certificatePem);
            String publicKeyFingerprint = generatePublicKeyFingerprint(cert);

            PartnerCertificate partnerCert = new PartnerCertificate();
            partnerCert.partner = partner;
            partnerCert.certificatePem = certificatePem;
            partnerCert.privateKeyPem = privateKeyPem;
            partnerCert.publicKeyFingerprint = publicKeyFingerprint;
            partnerCert.certificateType = cert.getType();
            partnerCert.keyAlgorithm = cert.getPublicKey().getAlgorithm();
            partnerCert.keySize = getKeySize(cert.getPublicKey());
            partnerCert.validFrom = LocalDateTime.ofInstant(cert.getNotBefore().toInstant(), ZoneId.systemDefault());
            partnerCert.validTo = LocalDateTime.ofInstant(cert.getNotAfter().toInstant(), ZoneId.systemDefault());
            partnerCert.issuer = cert.getIssuerX500Principal().getName();
            partnerCert.subject = cert.getSubjectX500Principal().getName();
            partnerCert.active = true;

            certificateRepository.persist(partnerCert);
            return partnerCert;
        } catch (Exception e) {
            throw new RuntimeException("Failed to add certificate", e);
        }
    }

    @Transactional
    public PartnerCertificate generateKeyPairAndStore(Long partnerId, int validityDays) throws Exception {
        Partner partner = partnerRepository.findById(partnerId);
        if (partner == null) {
            throw new IllegalArgumentException("Partner not found with id: " + partnerId);
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String publicKeyPem = publicKeyToPem(keyPair.getPublic());
        String privateKeyPem = privateKeyToPem(keyPair.getPrivate());
        String publicKeyFingerprint = generatePublicKeyFingerprint(keyPair.getPublic());

        PartnerCertificate partnerCert = new PartnerCertificate();
        partnerCert.partner = partner;
        partnerCert.certificatePem = publicKeyPem;
        partnerCert.privateKeyPem = privateKeyPem;
        partnerCert.publicKeyFingerprint = publicKeyFingerprint;
        partnerCert.certificateType = "RSA";
        partnerCert.keyAlgorithm = "RSA";
        partnerCert.keySize = 2048;
        partnerCert.validFrom = LocalDateTime.now();
        partnerCert.validTo = LocalDateTime.now().plusDays(validityDays);
        partnerCert.active = true;

        certificateRepository.persist(partnerCert);
        return partnerCert;
    }

    public Optional<PartnerCertificate> getActiveCertificate(Long partnerId) {
        return certificateRepository.findActiveByPartnerId(partnerId);
    }

    public Optional<PartnerCertificate> getValidCertificate(Long partnerId) {
        return certificateRepository.findValidByPartnerId(partnerId);
    }

    public List<PartnerCertificate> getCertificatesByPartner(Long partnerId) {
        return certificateRepository.findByPartnerId(partnerId);
    }

    public List<PartnerCertificate> getExpiringCertificates(Long partnerId, int daysUntilExpiry) {
        return certificateRepository.findExpiringSoon(partnerId, daysUntilExpiry);
    }

    public List<PartnerCertificate> getAllExpiredCertificates() {
        return certificateRepository.findExpiredCertificates();
    }

    @Transactional
    public boolean deactivateCertificate(Long certificateId) {
        PartnerCertificate cert = certificateRepository.findById(certificateId);
        if (cert == null) {
            return false;
        }
        cert.active = false;
        return true;
    }

    @Transactional
    public boolean deleteCertificate(Long certificateId) {
        return certificateRepository.deleteById(certificateId);
    }

    @Transactional
    public void deactivateAllPartnerCertificates(Long partnerId) {
        certificateRepository.deactivateByPartnerId(partnerId);
    }

    public boolean validateCertificate(Long certificateId) {
        PartnerCertificate cert = certificateRepository.findById(certificateId);
        if (cert == null) {
            return false;
        }
        return cert.isValid();
    }

    public boolean validateCertificateWithTrust(Long certificateId, String trustedCertPem) {
        try {
            PartnerCertificate cert = certificateRepository.findById(certificateId);
            if (cert == null || !cert.isValid()) {
                return false;
            }

            X509Certificate partnerCert = parseCertificate(cert.certificatePem);
            X509Certificate trustedCert = parseCertificate(trustedCertPem);

            try {
                partnerCert.verify(trustedCert.getPublicKey());
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifySignatureWithCertificate(Long certificateId, String data, String signatureB64) {
        try {
            PartnerCertificate cert = certificateRepository.findById(certificateId);
            if (cert == null || !cert.isValid()) {
                return false;
            }

            X509Certificate x509Cert = parseCertificate(cert.certificatePem);
            PublicKey publicKey = x509Cert.getPublicKey();

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private X509Certificate parseCertificate(String certificatePem) throws CertificateException {
        String cleanedPem = certificatePem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");

        byte[] certBytes = Base64.getDecoder().decode(cleanedPem);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    private String generatePublicKeyFingerprint(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(cert.getEncoded());
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate public key fingerprint", e);
        }
    }

    private String generatePublicKeyFingerprint(PublicKey publicKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(publicKey.getEncoded());
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate public key fingerprint", e);
        }
    }

    private String certificateToPem(X509Certificate cert) {
        try {
            String base64 = Base64.getEncoder().encodeToString(cert.getEncoded());
            return "-----BEGIN CERTIFICATE-----\n" +
                    base64.replaceAll("(.{64})", "$1\n") +
                    "\n-----END CERTIFICATE-----";
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new RuntimeException("Failed to encode certificate", e);
        }
    }

    private String publicKeyToPem(PublicKey publicKey) {
        String base64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" +
                base64.replaceAll("(.{64})", "$1\n") +
                "\n-----END PUBLIC KEY-----";
    }

    private String privateKeyToPem(PrivateKey privateKey) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" +
                base64.replaceAll("(.{64})", "$1\n") +
                "\n-----END PRIVATE KEY-----";
    }

    private int getKeySize(PublicKey publicKey) {
        if (publicKey instanceof java.security.interfaces.RSAPublicKey) {
            return ((java.security.interfaces.RSAPublicKey) publicKey).getModulus().bitLength();
        }
        return 0;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
