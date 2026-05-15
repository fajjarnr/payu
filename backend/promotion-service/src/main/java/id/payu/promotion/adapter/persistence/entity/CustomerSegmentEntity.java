package id.payu.promotion.adapter.persistence.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CustomerSegmentEntity entity for defining customer segmentation rules.
 * Segments can be used for personalized marketing campaigns and promotions.
 */
@Entity
@Table(name = "customer_segments", indexes = {
    @Index(name = "idx_segment_name", columnList = "name"),
    @Index(name = "idx_segment_active", columnList = "isActive")
})
@EntityListeners(AuditingEntityListener.class)
public class CustomerSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * JSONB field containing segment rules.
     * Rules can include:
     * - accountAge: minimum days since account creation
     * - transactionVolume: minimum transaction volume amount
     * - transactionCount: minimum number of transactions
     * - lastLoginDate: days since last login (null = no restriction)
     * - kycStatus: required KYC status (VERIFIED, PENDING, NOT_STARTED)
     * - loyaltyLevel: minimum loyalty level
     * - minBalance: minimum account balance
     * - maxBalance: maximum account balance
     * - registrationDateFrom: registration date range start
     * - registrationDateTo: registration date range end
     * Example: {"accountAge": 30, "transactionVolume": 1000000, "kycStatus": "VERIFIED", "loyaltyLevel": 3}
     */
    @Column(columnDefinition = "jsonb", nullable = false)
    private String rules;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (priority == null) {
            priority = 0;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
