package id.payu.backoffice.adapter.persistence.entity;

import id.payu.backoffice.domain.KycStatus;
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
public class KycReviewEntity {

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
    @Version
    private Long version;


    // Manual accessors for stability
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public KycStatus getStatus() { return status; }
    public void setStatus(KycStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }


    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = KycStatus.PENDING;
        }
    }
}
