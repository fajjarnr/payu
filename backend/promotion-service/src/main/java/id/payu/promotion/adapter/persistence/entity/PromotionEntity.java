package id.payu.promotion.adapter.persistence.entity;

import id.payu.promotion.domain.PromotionRewardType;
import id.payu.promotion.domain.PromotionStatus;
import id.payu.promotion.domain.PromotionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotions", indexes = {
    @Index(name = "idx_promotion_type", columnList = "promotionType"),
    @Index(name = "idx_promotion_status", columnList = "status"),
    @Index(name = "idx_promotion_dates", columnList = "startDate, endDate")
})
@EntityListeners(AuditingEntityListener.class)
public class PromotionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false)
    private PromotionType promotionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private PromotionRewardType rewardType;

    @Column(name = "reward_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal rewardValue;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "redemption_count")
    private Integer redemptionCount;

    @Column(name = "min_transaction_amount", precision = 19, scale = 4)
    private BigDecimal minTransactionAmount;

    @Column(name = "merchant_codes", columnDefinition = "JSONB")
    private String merchantCodes;

    @Column(name = "category_codes", columnDefinition = "JSONB")
    private String categoryCodes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PromotionStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (redemptionCount == null) {
            redemptionCount = 0;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PromotionType getPromotionType() { return promotionType; }
    public void setPromotionType(PromotionType promotionType) { this.promotionType = promotionType; }

    public PromotionRewardType getRewardType() { return rewardType; }
    public void setRewardType(PromotionRewardType rewardType) { this.rewardType = rewardType; }

    public BigDecimal getRewardValue() { return rewardValue; }
    public void setRewardValue(BigDecimal rewardValue) { this.rewardValue = rewardValue; }

    public Integer getMaxRedemptions() { return maxRedemptions; }
    public void setMaxRedemptions(Integer maxRedemptions) { this.maxRedemptions = maxRedemptions; }

    public Integer getRedemptionCount() { return redemptionCount; }
    public void setRedemptionCount(Integer redemptionCount) { this.redemptionCount = redemptionCount; }

    public BigDecimal getMinTransactionAmount() { return minTransactionAmount; }
    public void setMinTransactionAmount(BigDecimal minTransactionAmount) { this.minTransactionAmount = minTransactionAmount; }

    public String getMerchantCodes() { return merchantCodes; }
    public void setMerchantCodes(String merchantCodes) { this.merchantCodes = merchantCodes; }

    public String getCategoryCodes() { return categoryCodes; }
    public void setCategoryCodes(String categoryCodes) { this.categoryCodes = categoryCodes; }

    public PromotionStatus getStatus() { return status; }
    public void setStatus(PromotionStatus status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
