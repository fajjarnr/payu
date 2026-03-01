package id.payu.promotion.dto;

import java.math.BigDecimal;

/**
 * Request DTO for applying a promo code.
 */
public record ApplyPromoRequest(
        String promoCode,
        String userId,
        String transactionId,
        BigDecimal transactionAmount,
        String partnerId,
        String idempotencyKey
) {
    public ApplyPromoRequest(String promoCode, String userId, String transactionId,
                             BigDecimal transactionAmount, String partnerId) {
        this(promoCode, userId, transactionId, transactionAmount, partnerId, null);
    }
}
