package id.payu.promotion.adapter.persistence;

import id.payu.promotion.domain.model.PromoCode;
import id.payu.promotion.domain.port.out.PromoCodeRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Persistence adapter for PromoCode.
 * Implements the domain port using in-memory storage (for testing/demo).
 * In production, this would use JPA repository.
 */
@Component
public class PromoCodePersistenceAdapter implements PromoCodeRepositoryPort {

    private final ConcurrentMap<String, PromoCode> promoCodes = new ConcurrentHashMap<>();

    @Override
    public Optional<PromoCode> findByCode(String code) {
        return Optional.ofNullable(promoCodes.get(code));
    }

    @Override
    public PromoCode save(PromoCode promoCode) {
        promoCodes.put(promoCode.getCode(), promoCode);
        return promoCode;
    }

    public void clear() {
        promoCodes.clear();
    }
}
