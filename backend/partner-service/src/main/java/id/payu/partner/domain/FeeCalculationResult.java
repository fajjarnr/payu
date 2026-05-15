package id.payu.partner.domain;

import java.math.BigDecimal;

/**
 * Value object representing the result of a fee calculation.
 */
public class FeeCalculationResult {

    private final BigDecimal feeAmount;
    private final BigDecimal totalAmount;
    private final FeeType feeType;

    public FeeCalculationResult(BigDecimal feeAmount, BigDecimal totalAmount, FeeType feeType) {
        this.feeAmount = feeAmount;
        this.totalAmount = totalAmount;
        this.feeType = feeType;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public BigDecimal getNetAmount() {
        return totalAmount.subtract(feeAmount);
    }
}
