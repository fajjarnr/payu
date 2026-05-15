package id.payu.partner.adapter.persistence.entity;

import id.payu.partner.domain.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_certificates")
public class PartnerCertificateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private PartnerEntity partner;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String certificatePem;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String privateKeyPem;

    private String publicKeyFingerprint;

    private String certificateType;

    private String keyAlgorithm;

    private int keySize;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private boolean active;

    private String issuer;

    private String subject;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public PartnerCertificateEntity() {}

    public PartnerCertificateEntity(PartnerEntity partner, String certificatePem, String privateKeyPem,
                               String publicKeyFingerprint, String certificateType,
                               String keyAlgorithm, int keySize,
                               LocalDateTime validFrom, LocalDateTime validTo,
                               String issuer, String subject) {
        this.partner = partner;
        this.certificatePem = certificatePem;
        this.privateKeyPem = privateKeyPem;
        this.publicKeyFingerprint = publicKeyFingerprint;
        this.certificateType = certificateType;
        this.keyAlgorithm = keyAlgorithm;
        this.keySize = keySize;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.issuer = issuer;
        this.subject = subject;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PartnerEntity getPartner() { return partner; }
    public void setPartner(PartnerEntity partner) { this.partner = partner; }
    public String getCertificatePem() { return certificatePem; }
    public void setCertificatePem(String certificatePem) { this.certificatePem = certificatePem; }
    public String getPrivateKeyPem() { return privateKeyPem; }
    public void setPrivateKeyPem(String privateKeyPem) { this.privateKeyPem = privateKeyPem; }
    public String getPublicKeyFingerprint() { return publicKeyFingerprint; }
    public void setPublicKeyFingerprint(String publicKeyFingerprint) { this.publicKeyFingerprint = publicKeyFingerprint; }
    public String getCertificateType() { return certificateType; }
    public void setCertificateType(String certificateType) { this.certificateType = certificateType; }
    public String getKeyAlgorithm() { return keyAlgorithm; }
    public void setKeyAlgorithm(String keyAlgorithm) { this.keyAlgorithm = keyAlgorithm; }
    public int getKeySize() { return keySize; }
    public void setKeySize(int keySize) { this.keySize = keySize; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(validTo);
    }

    public boolean isNotYetValid() {
        return LocalDateTime.now().isBefore(validFrom);
    }

    public boolean isValid() {
        return !isExpired() && !isNotYetValid() && active;
    }
}
