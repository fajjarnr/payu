package id.payu.promotion.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event DTO representing a completed transaction.
 * Consumed from Kafka topic.
 */
public record TransactionCompletedEvent(
        String transactionId,
        String accountId,
        BigDecimal amount,
        String merchantCode,
        String categoryCode,
        Instant timestamp
) {
    public static Builder builder() {
        return new Builder();
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

        public TransactionCompletedEvent build() {
            return new TransactionCompletedEvent(transactionId, accountId, amount,
                    merchantCode, categoryCode, timestamp != null ? timestamp : Instant.now());
        }
    }
}
