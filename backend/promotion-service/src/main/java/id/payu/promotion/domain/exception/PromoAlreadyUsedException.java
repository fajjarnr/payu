package id.payu.promotion.domain.exception;

/**
 * Exception thrown when a user attempts to use a promo code they've already used.
 */
public class PromoAlreadyUsedException extends id.payu.api.common.exception.BusinessException {

    private final String promoCode;
    private final String userId;

    public PromoAlreadyUsedException(String promoCode, String userId) {
        super(String.format("Promo code '%s' has already been used by user '%s'", promoCode, userId));
        this.promoCode = promoCode;
        this.userId = userId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public String getUserId() {
        return userId;
    }
}
