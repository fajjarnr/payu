package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "referrals", indexes = {
    @Index(name = "idx_referral_referrer", columnList = "referrerAccountId"),
    @Index(name = "idx_referral_referee", columnList = "refereeAccountId"),
    @Index(name = "idx_referral_code", columnList = "referralCode"),
    @Index(name = "idx_referral_status", columnList = "status")
})
public class Referral extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "referrer_account_id", nullable = false)
    public String referrerAccountId;

    @Column(name = "referee_account_id")
    public String refereeAccountId;

    @Column(name = "referral_code", nullable = false, unique = true)
    public String referralCode;

    @Column(name = "referrer_reward", nullable = false, precision = 19, scale = 4)
    public BigDecimal referrerReward;

    @Column(name = "referee_reward", nullable = false, precision = 19, scale = 4)
    public BigDecimal refereeReward;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    public RewardType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public Status status;

    @Column(name = "completed_at")
    public LocalDateTime completedAt;

    @Column(name = "expiry_date")
    public LocalDateTime expiryDate;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = Status.PENDING;
        }
    }

    public enum RewardType {
        CASHBACK,
        POINTS,
        FIXED_AMOUNT
    }

    public enum Status {
        PENDING,
        COMPLETED,
        EXPIRED,
        CANCELLED
    }
}
