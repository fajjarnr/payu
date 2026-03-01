package id.payu.promotion.domain.model;

import java.math.BigDecimal;

/**
 * Value object representing the context of a transaction for promo code application.
 * Immutable by design.
 */
public class TransactionContext {

    private final String userId;
    private final BigDecimal amount;
    private final String partnerId;
    private final String transactionId;

    private TransactionContext(Builder builder) {
        this.userId = builder.userId;
        this.amount = builder.amount;
        this.partnerId = builder.partnerId;
        this.transactionId = builder.transactionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public static class Builder {
        private String userId;
        private BigDecimal amount;
        private String partnerId;
        private String transactionId;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder partnerId(String partnerId) {
            this.partnerId = partnerId;
            return this;
        }

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public TransactionContext build() {
            if (userId == null || amount == null) {
                throw new IllegalArgumentException("userId and amount are required");
            }
            return new TransactionContext(this);
        }
    }
}
