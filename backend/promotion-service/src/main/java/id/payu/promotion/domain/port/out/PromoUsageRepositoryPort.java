package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.PromoUsage;

import java.util.Optional;

public interface PromoUsageRepositoryPort {
    boolean hasUserUsedPromo(String userId, String promoCode);
    boolean recordUsage(PromoUsage usage);
    Optional<PromoUsage> findByIdempotencyKey(String idempotencyKey);
}
