package id.payu.backoffice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_reviews", indexes = {
        @Index(name = "idx_kyc_user", columnList = "userId"),
        @Index(name = "idx_kyc_status", columnList = "status"),
        @Index(name = "idx_kyc_reviewed_by", columnList = "reviewedBy")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(length = 100)
    private String documentType;

    @Column(length = 200)
    private String documentNumber;

    @Column(columnDefinition = "TEXT")
    private String documentUrl;

    @Column(length = 100)
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

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
