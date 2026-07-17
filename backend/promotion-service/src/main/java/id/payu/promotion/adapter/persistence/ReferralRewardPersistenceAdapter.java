package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import id.payu.promotion.adapter.persistence.repository.RewardRepository;
import id.payu.promotion.domain.RewardStatus;
import id.payu.promotion.domain.RewardType;
import id.payu.promotion.domain.TransactionType;
import id.payu.promotion.domain.port.out.ReferralRewardPort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class ReferralRewardPersistenceAdapter implements ReferralRewardPort {
    private final RewardRepository rewards;
    private final LoyaltyPointsRepository points;
    public ReferralRewardPersistenceAdapter(RewardRepository rewards, LoyaltyPointsRepository points) {
        this.rewards = rewards;
        this.points = points;
    }
    public void grantCashback(String accountId, BigDecimal amount, String transactionId) {
        RewardEntity reward = new RewardEntity();
        reward.setAccountId(accountId); reward.setTransactionId(transactionId);
        reward.setType(RewardType.REFERRAL_BONUS); reward.setAmount(amount);
        reward.setTransactionAmount(BigDecimal.ZERO); reward.setStatus(RewardStatus.AWARDED);
        rewards.save(reward);
    }
    public void grantPoints(String accountId, int amount, String transactionId, TransactionType type) {
        LoyaltyPointsEntity value = new LoyaltyPointsEntity();
        value.setAccountId(accountId); value.setTransactionId(transactionId);
        value.setTransactionType(type); value.setPoints(amount); value.setBalanceAfter(amount);
        points.save(value);
    }
}
