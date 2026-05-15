package id.payu.promotion.adapter.persistence.entity;

import id.payu.security.annotation.Sensitive;
import id.payu.security.annotation.SensitivityLevel;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SegmentMembershipEntity entity tracks the relationship between users and segments.
 * This allows efficient querying of which users belong to which segments.
 */
@Entity
@Table(name = "segment_memberships", indexes = {
    @Index(name = "idx_membership_account", columnList = "accountId"),
    @Index(name = "idx_membership_segment", columnList = "segmentId"),
    @Index(name = "idx_membership_account_segment", columnList = "accountId, segmentId"),
    @Index(name = "idx_membership_evaluated", columnList = "lastEvaluatedAt")
})
@EntityListeners(AuditingEntityListener.class)
public class SegmentMembershipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "segment_id", nullable = false)
    private UUID segmentId;

    /**
     * Indicates if the user currently matches the segment criteria.
     * This is updated during segment evaluation.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Timestamp of when this membership was last evaluated.
     * Used for re-evaluation scheduling.
     */
    @Column(name = "last_evaluated_at", nullable = false)
    private LocalDateTime lastEvaluatedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (lastEvaluatedAt == null) {
            lastEvaluatedAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public UUID getSegmentId() { return segmentId; }
    public void setSegmentId(UUID segmentId) { this.segmentId = segmentId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
