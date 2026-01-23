package id.payu.partner.dto;

import java.time.LocalDateTime;

public class PartnerCertificateDTO {
    public Long id;
    public Long partnerId;
    public String publicKeyFingerprint;
    public String certificateType;
    public String keyAlgorithm;
    public int keySize;
    public LocalDateTime validFrom;
    public LocalDateTime validTo;
    public boolean active;
    public String issuer;
    public String subject;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public PartnerCertificateDTO() {
    }

    public PartnerCertificateDTO(Long id, Long partnerId, String publicKeyFingerprint,
                                  String certificateType, String keyAlgorithm, int keySize,
                                  LocalDateTime validFrom, LocalDateTime validTo,
                                  boolean active, String issuer, String subject,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.publicKeyFingerprint = publicKeyFingerprint;
        this.certificateType = certificateType;
        this.keyAlgorithm = keyAlgorithm;
        this.keySize = keySize;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = active;
        this.issuer = issuer;
        this.subject = subject;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
