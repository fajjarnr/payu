package id.payu.promotion.adapter.persistence;

import id.payu.promotion.domain.model.PromoUsage;
import id.payu.promotion.domain.port.out.PromoUsageRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Persistence adapter for PromoUsage.
 * Implements the domain port using in-memory storage (for testing/demo).
 * In production, this would use JPA repository.
 */
@Component
public class PromoUsagePersistenceAdapter implements PromoUsageRepositoryPort {

    private final Set<String> userPromoUsage = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, PromoUsage> usagesByIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public boolean hasUserUsedPromo(String userId, String promoCode) {
        return userPromoUsage.contains(userId + ":" + promoCode);
    }

    @Override
    public boolean recordUsage(PromoUsage usage) {
        userPromoUsage.add(usage.getUserId() + ":" + usage.getPromoCode());
        if (usage.getIdempotencyKey() != null) {
            usagesByIdempotencyKey.put(usage.getIdempotencyKey(), usage);
        }
        return true;
    }

    @Override
    public Optional<PromoUsage> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(usagesByIdempotencyKey.get(idempotencyKey));
    }

    public void clear() {
        userPromoUsage.clear();
        usagesByIdempotencyKey.clear();
    }
}
