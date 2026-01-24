package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CustomerSegment entity for defining customer segmentation rules.
 * Segments can be used for personalized marketing campaigns and promotions.
 */
@Entity
@Table(name = "customer_segments", indexes = {
    @Index(name = "idx_segment_name", columnList = "name"),
    @Index(name = "idx_segment_active", columnList = "isActive")
})
public class CustomerSegment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(length = 500)
    public String description;

    /**
     * JSONB field containing segment rules.
     * Rules can include:
     * - accountAge: minimum days since account creation
     * - transactionVolume: minimum transaction volume amount
     * - transactionCount: minimum number of transactions
     * - lastLoginDate: days since last login (null = no restriction)
     * - kycStatus: required KYC status (VERIFIED, PENDING, NOT_STARTED)
     * - loyaltyLevel: minimum loyalty/gamification level
     * - hasBadges: list of required badge IDs (user must have all)
     * - minBalance: minimum account balance
     * - maxBalance: maximum account balance
     * - registrationDateFrom: registration date range start
     * - registrationDateTo: registration date range end
     * Example: {"accountAge": 30, "transactionVolume": 1000000, "kycStatus": "VERIFIED", "loyaltyLevel": 3}
     */
    @Column(columnDefinition = "jsonb", nullable = false)
    public String rules;

    @Column(nullable = false)
    public Boolean isActive = true;

    @Column(name = "priority", nullable = false)
    public Integer priority = 0;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (priority == null) {
            priority = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
