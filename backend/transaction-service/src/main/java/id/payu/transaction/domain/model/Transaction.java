package id.payu.transaction.domain.model;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class Transaction {
    public Transaction() {
    }

    public Transaction(UUID id, String referenceNumber, UUID senderAccountId, UUID recipientAccountId, TransactionType type, Money amount, BigDecimal amountValue, String currencyCode, String description, TransactionStatus status, String failureReason, String metadata, Instant createdAt, Instant updatedAt, Instant completedAt, String idempotencyKey, Instant expiresAt, String memo, String tags) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.type = type;
        this.amount = amount;
        this.amountValue = amountValue;
        this.currencyCode = currencyCode;
        this.description = description;
        this.status = status;
        this.failureReason = failureReason;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.idempotencyKey = idempotencyKey;
        this.expiresAt = expiresAt;
        this.memo = memo;
        this.tags = tags;
    }

    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public static class TransactionBuilder {
        private UUID id;
        private String referenceNumber;
        private UUID senderAccountId;
        private UUID recipientAccountId;
        private TransactionType type;
        private Money amount;
        private BigDecimal amountValue;
        private String currencyCode;
        private String description;
        private TransactionStatus status;
        private String failureReason;
        private String metadata;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant completedAt;
        private String idempotencyKey;
        private Instant expiresAt;
        private String memo;
        private String tags;

        public TransactionBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public TransactionBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public TransactionBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }
        public TransactionBuilder recipientAccountId(UUID recipientAccountId) {
            this.recipientAccountId = recipientAccountId;
            return this;
        }
        public TransactionBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }
        public TransactionBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }
        public TransactionBuilder amountValue(BigDecimal amountValue) {
            this.amountValue = amountValue;
            return this;
        }
        public TransactionBuilder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }
        public TransactionBuilder description(String description) {
            this.description = description;
            return this;
        }
        public TransactionBuilder status(TransactionStatus status) {
            this.status = status;
            return this;
        }
        public TransactionBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        public TransactionBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }
        public TransactionBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public TransactionBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public TransactionBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }
        public TransactionBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }
        public TransactionBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        public TransactionBuilder memo(String memo) {
            this.memo = memo;
            return this;
        }
        public TransactionBuilder tags(String tags) {
            this.tags = tags;
            return this;
        }

        public Transaction build() {
            return new Transaction(id, referenceNumber, senderAccountId, recipientAccountId, type, amount, amountValue, currencyCode, description, status, failureReason, metadata, createdAt, updatedAt, completedAt, idempotencyKey, expiresAt, memo, tags);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(UUID senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public UUID getRecipientAccountId() {
        return recipientAccountId;
    }

    public void setRecipientAccountId(UUID recipientAccountId) {
        this.recipientAccountId = recipientAccountId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

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

    public BigDecimal getAmountValue() {
        return amountValue;
    }

    public void setAmountValue(BigDecimal amountValue) {
        this.amountValue = amountValue;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Alias for getSenderAccountId() for compatibility with older code.
     * @return the source account ID (same as sender account ID)
     */
    public UUID getSourceAccountId() {
        return senderAccountId;
    }

    /**
     * Alias for getReferenceNumber() for compatibility with older code.
     * @return the reference ID (same as reference number)
     */
    public String getReferenceId() {
        return referenceNumber;
    }

    /**
     * Gets the currency code from the amount or currencyCode field.
     * @return the currency code
     */
    public String getCurrency() {
        if (amount != null) {
            return amount.getCurrency().getCurrencyCode();
        }
        return currencyCode;
    }

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

    @Column(name = "memo", length = 140)
    private String memo;

    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

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
