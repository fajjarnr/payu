package id.payu.partner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_certificates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Partner partner;

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
