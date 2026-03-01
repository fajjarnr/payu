package id.payu.promotion.dto;

import java.math.BigDecimal;

/**
 * Response DTO for applying a promo code.
 */
public record ApplyPromoResponse(
        boolean success,
        String promoCode,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String errorCode,
        String errorMessage
) {
    public static ApplyPromoResponse success(String promoCode, BigDecimal originalAmount,
                                              BigDecimal discountAmount, BigDecimal finalAmount) {
        return new ApplyPromoResponse(true, promoCode, originalAmount, discountAmount,
                finalAmount, null, null);
    }

    public static ApplyPromoResponse failure(String errorCode, String errorMessage) {
        return new ApplyPromoResponse(false, null, null, null, null, errorCode, errorMessage);
    }
}
