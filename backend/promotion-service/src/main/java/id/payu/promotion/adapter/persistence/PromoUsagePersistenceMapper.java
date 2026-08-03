package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.PromoUsageEntity;
import id.payu.promotion.domain.model.PromoUsage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Component
public class PromoUsagePersistenceMapper {

    public PromoUsage toDomain(PromoUsageEntity entity) {
        PromoUsage usage = new PromoUsage();
        usage.setId(entity.getId().toString());
        usage.setUserId(entity.getUserId());
        usage.setPromoCode(entity.getPromoCode());
        usage.setTransactionId(entity.getTransactionId());
        usage.setDiscountAmount(normalizeAmount(entity.getDiscountAmount()));
        usage.setFinalAmount(normalizeAmount(entity.getFinalAmount()));
        usage.setIdempotencyKey(entity.getIdempotencyKey());
        usage.setTimestamp(entity.getTimestamp());
        usage.setUsageType(entity.getUsageType());
        return usage;
    }

    public PromoUsageEntity toEntity(PromoUsage usage) {
        PromoUsageEntity entity = new PromoUsageEntity();
        if (usage.getId() != null) {
            entity.setId(UUID.fromString(usage.getId()));
        }
        entity.setUserId(usage.getUserId());
        entity.setPromoCode(usage.getPromoCode());
        entity.setTransactionId(usage.getTransactionId());
        entity.setDiscountAmount(usage.getDiscountAmount());
        entity.setFinalAmount(usage.getFinalAmount());
        entity.setIdempotencyKey(usage.getIdempotencyKey());
        entity.setTimestamp(usage.getTimestamp());
        entity.setUsageType(usage.getUsageType());
        return entity;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        int scale = Math.max(2, amount.stripTrailingZeros().scale());
        return amount.setScale(scale, RoundingMode.HALF_EVEN);
    }
}
