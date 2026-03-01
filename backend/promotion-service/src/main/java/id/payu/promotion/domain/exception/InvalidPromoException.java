package id.payu.promotion.domain.exception;

/**
 * Exception thrown when a promo code is invalid or cannot be applied.
 */
public class InvalidPromoException extends RuntimeException {

    private final String promoCode;
    private final String reason;

    public InvalidPromoException(String promoCode, String reason) {
        super(String.format("Promo code '%s' is invalid: %s", promoCode, reason));
        this.promoCode = promoCode;
        this.reason = reason;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public String getReason() {
        return reason;
    }
}
