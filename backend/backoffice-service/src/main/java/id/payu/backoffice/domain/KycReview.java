package id.payu.backoffice.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_reviews", indexes = {
        @Index(name = "idx_kyc_user", columnList = "userId"),
        @Index(name = "idx_kyc_status", columnList = "status"),
        @Index(name = "idx_kyc_reviewed_by", columnList = "reviewedBy")
})
public class KycReview extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String userId;

    @Column(nullable = false)
    public String accountNumber;

    @Column(length = 100)
    public String documentType;

    @Column(length = 200)
    public String documentNumber;

    @Column(columnDefinition = "TEXT")
    public String documentUrl;

    @Column(length = 100)
    public String fullName;

    @Column(columnDefinition = "TEXT")
    public String address;

    @Column(length = 50)
    public String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public KycStatus status;

    @Column(columnDefinition = "TEXT")
    public String notes;

    public String reviewedBy;

    public LocalDateTime reviewedAt;

    @Column(updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = KycStatus.PENDING;
        }
    }

    public enum KycStatus {
        PENDING,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        REQUIRES_ADDITIONAL_INFO
    }
}
