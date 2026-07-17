package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import id.payu.promotion.domain.model.Reward;
import org.springframework.stereotype.Component;

@Component
public class RewardPersistenceMapper {

    public Reward toDomain(RewardEntity entity) {
        return new Reward(
            entity.getId(), entity.getAccountId(), entity.getTransactionId(),
            entity.getPromotionCode(), entity.getType(), entity.getAmount(),
            entity.getPointsEarned(), entity.getTransactionAmount(),
            entity.getMerchantCode(), entity.getCategoryCode(), entity.getStatus(),
            entity.getExpiryDate(), entity.getCreatedAt(), entity.getVersion()
        );
    }

    public RewardEntity toEntity(Reward reward) {
        RewardEntity entity = new RewardEntity();
        entity.setId(reward.id());
        entity.setAccountId(reward.accountId());
        entity.setTransactionId(reward.transactionId());
        entity.setPromotionCode(reward.promotionCode());
        entity.setType(reward.type());
        entity.setAmount(reward.amount());
        entity.setPointsEarned(reward.pointsEarned());
        entity.setTransactionAmount(reward.transactionAmount());
        entity.setMerchantCode(reward.merchantCode());
        entity.setCategoryCode(reward.categoryCode());
        entity.setStatus(reward.status());
        entity.setExpiryDate(reward.expiryDate());
        entity.setCreatedAt(reward.createdAt());
        entity.setVersion(reward.version());
        return entity;
    }
}
