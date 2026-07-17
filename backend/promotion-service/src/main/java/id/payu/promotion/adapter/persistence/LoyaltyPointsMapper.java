package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.domain.model.LoyaltyPoints;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyPointsMapper {
    public LoyaltyPoints toDomain(LoyaltyPointsEntity entity) {
        LoyaltyPoints value = new LoyaltyPoints();
        value.setId(entity.getId());
        value.setAccountId(entity.getAccountId());
        value.setTransactionId(entity.getTransactionId());
        value.setTransactionType(entity.getTransactionType());
        value.setPoints(entity.getPoints());
        value.setBalanceAfter(entity.getBalanceAfter());
        value.setExpiryDate(entity.getExpiryDate());
        value.setRedeemedAt(entity.getRedeemedAt());
        value.setVersion(entity.getVersion());
        value.setCreatedAt(entity.getCreatedAt());
        return value;
    }

    public LoyaltyPointsEntity toEntity(LoyaltyPoints value) {
        LoyaltyPointsEntity entity = new LoyaltyPointsEntity();
        entity.setId(value.getId());
        entity.setAccountId(value.getAccountId());
        entity.setTransactionId(value.getTransactionId());
        entity.setTransactionType(value.getTransactionType());
        entity.setPoints(value.getPoints());
        entity.setBalanceAfter(value.getBalanceAfter());
        entity.setExpiryDate(value.getExpiryDate());
        entity.setRedeemedAt(value.getRedeemedAt());
        entity.setVersion(value.getVersion());
        entity.setCreatedAt(value.getCreatedAt());
        return entity;
    }
}
