package id.payu.partner.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "partner_certificates")
public class PartnerCertificate extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    public Partner partner;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    public String certificatePem;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    public String privateKeyPem;

    public String publicKeyFingerprint;

    public String certificateType;

    public String keyAlgorithm;

    public int keySize;

    public LocalDateTime validFrom;

    public LocalDateTime validTo;

    public boolean active;

    public String issuer;

    public String subject;

    @CreationTimestamp
    public LocalDateTime createdAt;

    @UpdateTimestamp
    public LocalDateTime updatedAt;

    public PartnerCertificate() {
    }

    public PartnerCertificate(Partner partner, String certificatePem, String privateKeyPem,
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
