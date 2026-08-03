package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.PromoCodeEntity;
import id.payu.promotion.domain.model.PromoCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class PromoCodePersistenceMapper {

    public PromoCode toDomain(PromoCodeEntity entity) {
        PromoCode promo = new PromoCode();
        promo.setCode(entity.getCode());
        promo.setDiscountValue(entity.getDiscountValue());
        promo.setDiscountType(entity.getDiscountType());
        promo.setUsageType(entity.getUsageType());
        promo.setStatus(entity.getStatus());
        promo.setMinimumAmount(entity.getMinimumAmount());
        promo.setMaxDiscountAmount(entity.getMaxDiscountAmount());
        promo.setMaxUsageCount(entity.getMaxUsageCount());
        promo.setCurrentUsageCount(entity.getCurrentUsageCount());
        promo.setExpiryDate(entity.getExpiryDate());
        promo.setExcludedPartnerIds(entity.getExcludedPartnerIds() == null
                ? new HashSet<>() : new HashSet<>(entity.getExcludedPartnerIds()));
        return promo;
    }

    public void updateEntity(PromoCodeEntity entity, PromoCode promo) {
        entity.setCode(promo.getCode());
        entity.setDiscountValue(promo.getDiscountValue());
        entity.setDiscountType(promo.getDiscountType());
        entity.setUsageType(promo.getUsageType());
        entity.setStatus(promo.getStatus());
        entity.setMinimumAmount(promo.getMinimumAmount());
        entity.setMaxDiscountAmount(promo.getMaxDiscountAmount());
        entity.setMaxUsageCount(promo.getMaxUsageCount());
        entity.setCurrentUsageCount(promo.getCurrentUsageCount());
        entity.setExpiryDate(promo.getExpiryDate());
        entity.setExcludedPartnerIds(promo.getExcludedPartnerIds() == null
                ? new HashSet<>() : new HashSet<>(promo.getExcludedPartnerIds()));
    }
}
