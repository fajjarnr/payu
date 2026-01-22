package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rewards", indexes = {
    @Index(name = "idx_reward_account", columnList = "accountId"),
    @Index(name = "idx_reward_transaction", columnList = "transactionId"),
    @Index(name = "idx_reward_date", columnList = "createdAt DESC")
})
public class Reward extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public String accountId;

    @Column(name = "transaction_id")
    public String transactionId;

    @Column(name = "promotion_code")
    public String promotionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    public RewardType type;

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal amount;

    @Column(name = "points_earned")
    public Integer pointsEarned;

    @Column(name = "transaction_amount", nullable = false, precision = 19, scale = 4)
    public BigDecimal transactionAmount;

    @Column(name = "merchant_code")
    public String merchantCode;

    @Column(name = "category_code")
    public String categoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public Status status;

    @Column(name = "expiry_date")
    public LocalDateTime expiryDate;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RewardType {
        CASHBACK,
        LOYALTY_POINTS,
        REFERRAL_BONUS,
        PROMOTION_REWARD
    }

    public enum Status {
        PENDING,
        AWARDED,
        EXPIRED,
        VOIDED
    }
}
