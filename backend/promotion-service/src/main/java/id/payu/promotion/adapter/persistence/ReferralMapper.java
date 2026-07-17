package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.ReferralEntity;
import id.payu.promotion.domain.model.Referral;
import org.springframework.stereotype.Component;

@Component
public class ReferralMapper {
    public Referral toDomain(ReferralEntity entity) {
        Referral value = new Referral();
        value.setId(entity.getId());
        value.setReferrerAccountId(entity.getReferrerAccountId());
        value.setRefereeAccountId(entity.getRefereeAccountId());
        value.setReferralCode(entity.getReferralCode());
        value.setReferrerReward(entity.getReferrerReward());
        value.setRefereeReward(entity.getRefereeReward());
        value.setRewardType(entity.getRewardType());
        value.setStatus(entity.getStatus());
        value.setCompletedAt(entity.getCompletedAt());
        value.setExpiryDate(entity.getExpiryDate());
        value.setCreatedAt(entity.getCreatedAt());
        value.setVersion(entity.getVersion());
        return value;
    }

    public ReferralEntity toEntity(Referral value) {
        ReferralEntity entity = new ReferralEntity();
        entity.setId(value.getId());
        entity.setReferrerAccountId(value.getReferrerAccountId());
        entity.setRefereeAccountId(value.getRefereeAccountId());
        entity.setReferralCode(value.getReferralCode());
        entity.setReferrerReward(value.getReferrerReward());
        entity.setRefereeReward(value.getRefereeReward());
        entity.setRewardType(value.getRewardType());
        entity.setStatus(value.getStatus());
        entity.setCompletedAt(value.getCompletedAt());
        entity.setExpiryDate(value.getExpiryDate());
        entity.setCreatedAt(value.getCreatedAt());
        entity.setVersion(value.getVersion());
        return entity;
    }
}
