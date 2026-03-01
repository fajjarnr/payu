package id.payu.promotion.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Value object representing the result of applying a promo code.
 * Immutable by design.
 */
public class PromoResult {

    private final boolean success;
    private final String promoCode;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal finalAmount;
    private final String message;
    private final Instant appliedAt;

    private PromoResult(Builder builder) {
        this.success = builder.success;
        this.promoCode = builder.promoCode;
        this.originalAmount = builder.originalAmount;
        this.discountAmount = builder.discountAmount;
        this.finalAmount = builder.finalAmount;
        this.message = builder.message;
        this.appliedAt = builder.appliedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public String getMessage() {
        return message;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    /**
     * Creates a successful promo result.
     */
    public static PromoResult success(String promoCode, BigDecimal originalAmount,
                                       BigDecimal discountAmount, BigDecimal finalAmount) {
        return builder()
                .success(true)
                .promoCode(promoCode)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .appliedAt(Instant.now())
                .build();
    }

    /**
     * Creates a failed promo result.
     */
    public static PromoResult failure(String promoCode, String message) {
        return builder()
                .success(false)
                .promoCode(promoCode)
                .message(message)
                .appliedAt(Instant.now())
                .build();
    }

    public static class Builder {
        private boolean success;
        private String promoCode;
        private BigDecimal originalAmount;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private String message;
        private Instant appliedAt;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder promoCode(String promoCode) {
            this.promoCode = promoCode;
            return this;
        }

        public Builder originalAmount(BigDecimal originalAmount) {
            this.originalAmount = originalAmount;
            return this;
        }

        public Builder discountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
            return this;
        }

        public Builder finalAmount(BigDecimal finalAmount) {
            this.finalAmount = finalAmount;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder appliedAt(Instant appliedAt) {
            this.appliedAt = appliedAt;
            return this;
        }

        public PromoResult build() {
            return new PromoResult(this);
        }
    }
}
