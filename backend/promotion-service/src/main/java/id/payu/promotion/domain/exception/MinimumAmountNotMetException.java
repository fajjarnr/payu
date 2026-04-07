package id.payu.promotion.domain.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when transaction amount is below the minimum required for a promo.
 */
public class MinimumAmountNotMetException extends id.payu.api.common.exception.BusinessException {

    private final String promoCode;
    private final BigDecimal requiredAmount;
    private final BigDecimal actualAmount;

    public MinimumAmountNotMetException(String promoCode, BigDecimal requiredAmount, BigDecimal actualAmount) {
        super("PRM_VAL_002", String.format("Promo '%s' requires minimum transaction of %s, but got %s",
                promoCode, requiredAmount, actualAmount));
        this.promoCode = promoCode;
        this.requiredAmount = requiredAmount;
        this.actualAmount = actualAmount;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }
}
