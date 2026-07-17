package id.payu.promotion.domain.model;

import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.ReferralStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Referral {
    private UUID id;
    private String referrerAccountId;
    private String refereeAccountId;
    private String referralCode;
    private BigDecimal referrerReward;
    private BigDecimal refereeReward;
    private ReferralRewardType rewardType;
    private ReferralStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getReferrerAccountId() { return referrerAccountId; }
    public void setReferrerAccountId(String value) { this.referrerAccountId = value; }
    public String getRefereeAccountId() { return refereeAccountId; }
    public void setRefereeAccountId(String value) { this.refereeAccountId = value; }
    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String value) { this.referralCode = value; }
    public BigDecimal getReferrerReward() { return referrerReward; }
    public void setReferrerReward(BigDecimal value) { this.referrerReward = value; }
    public BigDecimal getRefereeReward() { return refereeReward; }
    public void setRefereeReward(BigDecimal value) { this.refereeReward = value; }
    public ReferralRewardType getRewardType() { return rewardType; }
    public void setRewardType(ReferralRewardType value) { this.rewardType = value; }
    public ReferralStatus getStatus() { return status; }
    public void setStatus(ReferralStatus value) { this.status = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime value) { this.expiryDate = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { this.version = value; }
}
