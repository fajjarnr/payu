package id.payu.promotion.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Value object representing a transaction for cashback rule matching.
 * Immutable by design.
 */
public class Transaction {

    private final String transactionId;
    private final String accountId;
    private final BigDecimal amount;
    private final String merchantCode;
    private final String categoryCode;
    private final Instant timestamp;

    private Transaction(Builder builder) {
        this.transactionId = builder.transactionId;
        this.accountId = builder.accountId;
        this.amount = builder.amount;
        this.merchantCode = builder.merchantCode;
        this.categoryCode = builder.categoryCode;
        this.timestamp = builder.timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public static class Builder {
        private String transactionId;
        private String accountId;
        private BigDecimal amount;
        private String merchantCode;
        private String categoryCode;
        private Instant timestamp;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder merchantCode(String merchantCode) {
            this.merchantCode = merchantCode;
            return this;
        }

        public Builder categoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Transaction build() {
            if (transactionId == null || accountId == null || amount == null) {
                throw new IllegalArgumentException("transactionId, accountId, and amount are required");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            return new Transaction(this);
        }
    }
}
