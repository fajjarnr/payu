package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.repository.PromoUsageRepository;
import id.payu.promotion.domain.model.PromoUsage;
import id.payu.promotion.domain.model.UsageType;
import id.payu.promotion.domain.port.out.PromoUsageRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class PromoUsagePersistenceAdapter implements PromoUsageRepositoryPort {

    private final PromoUsageRepository repository;
    private final PromoUsagePersistenceMapper mapper;

    public PromoUsagePersistenceAdapter(PromoUsageRepository repository, PromoUsagePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean hasUserUsedPromo(String userId, String promoCode) {
        return repository.existsByUserIdAndPromoCode(userId, promoCode);
    }

    @Override
    @Transactional
    public boolean recordUsage(PromoUsage usage) {
        if (usage.getUsageType() == UsageType.ONCE_PER_USER
                && repository.existsByUserIdAndPromoCode(usage.getUserId(), usage.getPromoCode())) {
            return false;
        }
        UsageType usageType = usage.getUsageType() == null ? UsageType.UNLIMITED : usage.getUsageType();
        return repository.insertIgnore(
                usage.getId(),
                usage.getUserId(),
                usage.getPromoCode(),
                usage.getTransactionId(),
                usage.getDiscountAmount(),
                usage.getFinalAmount(),
                usage.getIdempotencyKey(),
                usage.getTimestamp(),
                usageType.name()) > 0;
    }

    @Override
    public Optional<PromoUsage> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Transactional
    public void clear() {
        repository.deleteAllInBatch();
    }
}
