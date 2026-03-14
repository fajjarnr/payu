package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.PromoCode;

import java.util.Optional;

public interface PromoCodeRepositoryPort {
    Optional<PromoCode> findByCode(String code);
    PromoCode save(PromoCode promoCode);
}
