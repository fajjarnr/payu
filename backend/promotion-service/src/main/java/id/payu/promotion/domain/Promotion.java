package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotions", indexes = {
    @Index(name = "idx_promotion_type", columnList = "promotionType"),
    @Index(name = "idx_promotion_status", columnList = "status"),
    @Index(name = "idx_promotion_dates", columnList = "startDate, endDate")
})
public class Promotion extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "code", nullable = false, unique = true)
    public String code;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false)
    public PromotionType promotionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    public RewardType rewardType;

    @Column(name = "reward_value", nullable = false, precision = 19, scale = 4)
    public BigDecimal rewardValue;

    @Column(name = "max_redemptions")
    public Integer maxRedemptions;

    @Column(name = "redemption_count")
    public Integer redemptionCount;

    @Column(name = "min_transaction_amount", precision = 19, scale = 4)
    public BigDecimal minTransactionAmount;

    @Column(name = "merchant_codes", columnDefinition = "JSONB")
    public String merchantCodes;

    @Column(name = "category_codes", columnDefinition = "JSONB")
    public String categoryCodes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public Status status;

    @Column(name = "start_date", nullable = false)
    public LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDateTime endDate;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (redemptionCount == null) {
            redemptionCount = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PromotionType {
        CASHBACK,
        DISCOUNT,
        REWARD_POINTS,
        REFERRAL_BONUS
    }

    public enum RewardType {
        PERCENTAGE,
        FIXED_AMOUNT,
        POINTS
    }

    public enum Status {
        DRAFT,
        ACTIVE,
        PAUSED,
        EXPIRED,
        CANCELLED
    }
}
