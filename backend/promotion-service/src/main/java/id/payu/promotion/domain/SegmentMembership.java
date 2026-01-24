package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SegmentMembership entity tracks the relationship between users and segments.
 * This allows efficient querying of which users belong to which segments.
 */
@Entity
@Table(name = "segment_memberships", indexes = {
    @Index(name = "idx_membership_account", columnList = "accountId"),
    @Index(name = "idx_membership_segment", columnList = "segmentId"),
    @Index(name = "idx_membership_account_segment", columnList = "accountId, segmentId"),
    @Index(name = "idx_membership_evaluated", columnList = "lastEvaluatedAt")
})
public class SegmentMembership extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public String accountId;

    @Column(name = "segment_id", nullable = false)
    public UUID segmentId;

    /**
     * Indicates if the user currently matches the segment criteria.
     * This is updated during segment evaluation.
     */
    @Column(name = "is_active", nullable = false)
    public Boolean isActive = true;

    /**
     * Timestamp of when this membership was last evaluated.
     * Used for re-evaluation scheduling.
     */
    @Column(name = "last_evaluated_at", nullable = false)
    public LocalDateTime lastEvaluatedAt;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        lastEvaluatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
}
