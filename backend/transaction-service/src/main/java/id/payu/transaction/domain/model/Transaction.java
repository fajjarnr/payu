package id.payu.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private UUID id;
    private String referenceNumber;
    private UUID senderAccountId;
    private UUID recipientAccountId;
    private TransactionType type;

    /**
     * The monetary amount involved in this transaction.
     * Uses Money Value Object for precise decimal arithmetic and currency safety.
     */
    private Money amount;

    /**
     * @deprecated Use {@link #getAmount()} instead. This field is kept for JPA compatibility.
     */
    @Deprecated
    private BigDecimal amountValue;

    /**
     * @deprecated Use {@link #getAmount()} instead. This field is kept for JPA compatibility.
     */
    @Deprecated
    private String currencyCode;

    private String description;
    private TransactionStatus status;
    private String failureReason;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private String idempotencyKey;

    /**
     * Gets the monetary amount.
     * For backward compatibility, reconstructs Money from deprecated fields if amount is null.
     *
     * @return the monetary amount
     */
    public Money getAmount() {
        if (amount == null && amountValue != null && currencyCode != null) {
            return Money.of(amountValue, currencyCode);
        }
        return amount;
    }

    /**
     * Sets the monetary amount.
     * Also updates deprecated fields for JPA compatibility.
     *
     * @param amount the monetary amount
     */
    public void setAmount(Money amount) {
        this.amount = amount;
        if (amount != null) {
            this.amountValue = amount.getAmount();
            this.currencyCode = amount.getCurrency().getCurrencyCode();
        }
    }

    public enum TransactionType {
        INTERNAL_TRANSFER,
        BIFAST_TRANSFER,
        SKN_TRANSFER,
        RTGS_TRANSFER,
        QRIS_PAYMENT,
        BILL_PAYMENT,
        TOP_UP
    }

    public enum TransactionStatus {
        PENDING,
        VALIDATING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
