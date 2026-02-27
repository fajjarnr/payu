package id.payu.transaction.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @Column(name = "sender_account_id", nullable = false)
    private UUID senderAccountId;

    @Column(name = "recipient_account_id")
    private UUID recipientAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /**
     * The monetary amount involved in this transaction.
     * Uses Money Value Object for precise decimal arithmetic and currency safety.
     * Transient because we persist amountValue and currencyCode instead.
     */
    @Transient
    private Money amount;

    /**
     * @deprecated Use {@link #getAmount()} instead. This field is kept for JPA compatibility.
     * Mapped to 'amount' column in database.
     */
    @Deprecated
    @Column(name = "amount")
    private BigDecimal amountValue;

    /**
     * @deprecated Use {@link #getAmount()} instead. This field is kept for JPA compatibility.
     * Mapped to 'currency' column in database.
     */
    @Deprecated
    @Column(name = "currency")
    private String currencyCode;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "expires_at")
    private Instant expiresAt;

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
