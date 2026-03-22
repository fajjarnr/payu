package id.payu.promotion.domain.exception;

/**
 * Exception thrown when attempting to apply an expired promo code.
 */
public class PromoExpiredException extends id.payu.api.common.exception.BusinessException {

    private final String promoCode;

    public PromoExpiredException(String promoCode) {
        super(String.format("Promo code '%s' has expired", promoCode));
        this.promoCode = promoCode;
    }

    public String getPromoCode() {
        return promoCode;
    }
}
