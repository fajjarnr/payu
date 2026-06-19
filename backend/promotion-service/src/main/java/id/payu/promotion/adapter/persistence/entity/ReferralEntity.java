package id.payu.promotion.adapter.persistence.entity;

import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.ReferralStatus;
import id.payu.security.annotation.Sensitive;
import id.payu.security.annotation.SensitivityLevel;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
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
@EntityListeners(AuditingEntityListener.class)
public class ReferralEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "referrer_account_id", nullable = false)
    private String referrerAccountId;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "referee_account_id")
    private String refereeAccountId;

    @Column(name = "referral_code", nullable = false, unique = true)
    private String referralCode;

    @Column(name = "referrer_reward", nullable = false, precision = 19, scale = 4)
    private BigDecimal referrerReward;

    @Column(name = "referee_reward", nullable = false, precision = 19, scale = 4)
    private BigDecimal refereeReward;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private ReferralRewardType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReferralStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Version
    private Long version;


    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ReferralStatus.PENDING;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getReferrerAccountId() { return referrerAccountId; }
    public void setReferrerAccountId(String referrerAccountId) { this.referrerAccountId = referrerAccountId; }

    public String getRefereeAccountId() { return refereeAccountId; }
    public void setRefereeAccountId(String refereeAccountId) { this.refereeAccountId = refereeAccountId; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public BigDecimal getReferrerReward() { return referrerReward; }
    public void setReferrerReward(BigDecimal referrerReward) { this.referrerReward = referrerReward; }

    public BigDecimal getRefereeReward() { return refereeReward; }
    public void setRefereeReward(BigDecimal refereeReward) { this.refereeReward = refereeReward; }

    public ReferralRewardType getRewardType() { return rewardType; }
    public void setRewardType(ReferralRewardType rewardType) { this.rewardType = rewardType; }

    public ReferralStatus getStatus() { return status; }
    public void setStatus(ReferralStatus status) { this.status = status; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
