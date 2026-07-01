package id.payu.transaction.adapter.persistence.entity;

import id.payu.transaction.domain.model.*;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate Root representing a disbursement (payout) transaction.
 *
 * <p>A disbursement represents a transfer of funds from a PayU wallet/source account
 * to an external bank account via BI-FAST or other transfer networks. This entity
 * encapsulates the full lifecycle of a payout operation including idempotency handling,
 * state transitions, and bank reference tracking.</p>
 *
 * <p>State Machine:</p>
 * <pre>
 * PENDING → PROCESSING → COMPLETED
 *                    ↘ FAILED
 * </pre>
 *
 * <p>Key Business Rules:</p>
 * <ul>
 *   <li>Amount must be positive</li>
 *   <li>Bank code, account number, and account name are required</li>
 *   <li>Idempotency key ensures duplicate protection</li>
 *   <li>State transitions are strictly controlled</li>
 *   <li>Only PROCESSING disbursements can be completed or failed</li>
 * </ul>
 *
 * <p>PCI-DSS Compliance:</p>
 * <ul>
 *   <li>Uses Money Value Object for precise decimal arithmetic</li>
 *   <li>No sensitive card data stored</li>
 *   <li>Audit trail via createdAt, processedAt, completedAt timestamps</li>
 * </ul>
 *
 * @see DisbursementStatus
 * @see Money
 */
@Entity
@Table(name = "disbursements", indexes = {
    @Index(name = "idx_disbursement_source_account", columnList = "source_account_id"),
    @Index(name = "idx_disbursement_status", columnList = "status"),
    @Index(name = "idx_disbursement_created_at", columnList = "created_at"),
    @Index(name = "idx_disbursement_idempotency", columnList = "idempotency_key", unique = true)
})
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class DisbursementEntity {
    public DisbursementEntity() {
    }

    public DisbursementEntity(UUID id, String idempotencyKey, UUID sourceAccountId, Money amount, BigDecimal amountValue, String currencyCode, String bankCode, String accountNumber, String accountName, String description, DisbursementStatus status, String bankReference, String failureReason, Instant createdAt, Instant processedAt, Instant completedAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.amount = amount;
        this.amountValue = amountValue;
        this.currencyCode = currencyCode;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.description = description;
        this.status = status;
        this.bankReference = bankReference;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.completedAt = completedAt;
    }

    public static DisbursementEntityBuilder builder() {
        return new DisbursementEntityBuilder();
    }

    public static class DisbursementEntityBuilder {
        private UUID id;
        private String idempotencyKey;
        private UUID sourceAccountId;
        private Money amount;
        private BigDecimal amountValue;
        private String currencyCode;
        private String bankCode;
        private String accountNumber;
        private String accountName;
        private String description;
        private DisbursementStatus status;
        private String bankReference;
        private String failureReason;
        private Instant createdAt;
        private Instant processedAt;
        private Instant completedAt;

        public DisbursementEntityBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public DisbursementEntityBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }
        public DisbursementEntityBuilder sourceAccountId(UUID sourceAccountId) {
            this.sourceAccountId = sourceAccountId;
            return this;
        }
        public DisbursementEntityBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }
        public DisbursementEntityBuilder amountValue(BigDecimal amountValue) {
            this.amountValue = amountValue;
            return this;
        }
        public DisbursementEntityBuilder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }
        public DisbursementEntityBuilder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }
        public DisbursementEntityBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }
        public DisbursementEntityBuilder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }
        public DisbursementEntityBuilder description(String description) {
            this.description = description;
            return this;
        }
        public DisbursementEntityBuilder status(DisbursementStatus status) {
            this.status = status;
            return this;
        }
        public DisbursementEntityBuilder bankReference(String bankReference) {
            this.bankReference = bankReference;
            return this;
        }
        public DisbursementEntityBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        public DisbursementEntityBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public DisbursementEntityBuilder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }
        public DisbursementEntityBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public DisbursementEntity build() {
            return new DisbursementEntity(id, idempotencyKey, sourceAccountId, amount, amountValue, currencyCode, bankCode, accountNumber, accountName, description, status, bankReference, failureReason, createdAt, processedAt, completedAt);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
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

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DisbursementStatus getStatus() {
        return status;
    }

    public void setStatus(DisbursementStatus status) {
        this.status = status;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }



    // Application-assigned UUID. No @GeneratedValue — manual ID assignment
    // would conflict with @GeneratedValue detection (Hibernate would treat
    // the entity as "previously persisted" and call merge() instead of
    // persist() for new rows, causing StaleObjectStateException).
    @Id
    private UUID id;

    /**
     * Optimistic locking version. Spring Data JPA uses this field to determine
     * if an entity is new (isNew=true if version is null), which routes save() to
     * EntityManager.persist() (INSERT) instead of merge() (UPDATE/INSERT for detached).
     *
     * <p>Per Spring Data JPA detection strategy: when a non-primitive @Version
     * property exists, the entity is considered new when its value is null.
     * This allows manual id assignment to coexist with @GeneratedValue UUID.</p>
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    /**
     * The monetary amount to disburse.
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
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountValue; // AUDIT-042

    /**
     * @deprecated Use {@link #getAmount()} instead. This field is kept for JPA compatibility.
     * Mapped to 'currency' column in database.
     */
    @Deprecated
    @Column(name = "currency", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisbursementStatus status;

    @Column(name = "bank_reference", length = 50)
    private String bankReference;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Creates a new disbursement with the specified parameters.
     * Generates a unique idempotency key automatically.
     *
     * @param sourceAccountId the source wallet/account ID
     * @param amount the amount to disburse
     * @param bankCode the destination bank code (e.g., "014" for BCA)
     * @param accountNumber the destination account number
     * @param accountName the destination account name
     * @return a new DisbursementEntity in PENDING status
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public static DisbursementEntity create(UUID sourceAccountId, Money amount, String bankCode,
                                      String accountNumber, String accountName) {
        return createWithIdempotencyKey(sourceAccountId, amount, bankCode, accountNumber,
                accountName, generateIdempotencyKey());
    }

    /**
     * Creates a new disbursement with a specific idempotency key.
     *
     * @param sourceAccountId the source wallet/account ID
     * @param amount the amount to disburse
     * @param bankCode the destination bank code
     * @param accountNumber the destination account number
     * @param accountName the destination account name
     * @param idempotencyKey the idempotency key for duplicate protection
     * @return a new DisbursementEntity in PENDING status
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public static DisbursementEntity createWithIdempotencyKey(UUID sourceAccountId, Money amount,
                                                        String bankCode, String accountNumber,
                                                        String accountName, String idempotencyKey) {
        validateSourceAccountId(sourceAccountId);
        validateAmount(amount);
        validateBankCode(bankCode);
        validateAccountNumber(accountNumber);
        validateAccountName(accountName);
        validateIdempotencyKey(idempotencyKey);

        DisbursementEntity disbursement = new DisbursementEntity();
        // Application-assigned UUID (no @GeneratedValue). Hibernate will persist
        // as INSERT because isNew() detection sees id != null + no @Version field.
        // Wallet service uses this stable id as transactionId for the reservation.
        disbursement.id = UUID.randomUUID();
        disbursement.idempotencyKey = idempotencyKey;
        disbursement.sourceAccountId = sourceAccountId;
        disbursement.setAmount(amount);
        disbursement.bankCode = bankCode;
        disbursement.accountNumber = accountNumber;
        disbursement.accountName = accountName;
        disbursement.status = DisbursementStatus.PENDING;
        disbursement.createdAt = Instant.now();

        return disbursement;
    }

    /**
     * Transitions the disbursement from PENDING to PROCESSING.
     * This should be called after funds have been debited from the source account.
     *
     * @throws IllegalStateException if disbursement is not in PENDING status
     */
    public void process() {
        if (status != DisbursementStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot process disbursement in status: " + status + ". Expected: PENDING"
            );
        }
        this.status = DisbursementStatus.PROCESSING;
        this.processedAt = Instant.now();
    }

    /**
     * Completes the disbursement with a bank reference.
     * This should be called when BI-FAST confirms successful transfer.
     *
     * @param bankReference the reference number from the bank
     * @throws IllegalStateException if disbursement is not in PROCESSING status
     * @throws IllegalArgumentException if bank reference is null or empty
     */
    public void complete(String bankReference) {
        if (status != DisbursementStatus.PROCESSING) {
            throw new IllegalStateException(
                "Cannot complete disbursement in status: " + status + ". Expected: PROCESSING"
            );
        }
        if (bankReference == null || bankReference.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank reference cannot be null or empty");
        }
        this.status = DisbursementStatus.COMPLETED;
        this.bankReference = bankReference;
        this.completedAt = Instant.now();
    }

    /**
     * Marks the disbursement as failed.
     * This should be called when BI-FAST reports a failed transfer.
     *
     * @param reason the failure reason
     * @throws IllegalStateException if disbursement is not in PROCESSING status
     * @throws IllegalArgumentException if reason is null or empty
     */
    public void fail(String reason) {
        if (status != DisbursementStatus.PROCESSING) {
            throw new IllegalStateException(
                "Cannot fail disbursement in status: " + status + ". Expected: PROCESSING"
            );
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure reason cannot be null or empty");
        }
        this.status = DisbursementStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = Instant.now();
    }

    /**
     * Checks if this disbursement matches the given idempotency key.
     *
     * @param key the idempotency key to check
     * @return true if the keys match, false otherwise
     */
    public boolean matchesIdempotencyKey(String key) {
        return key != null && key.equals(this.idempotencyKey);
    }

    /**
     * Checks if the disbursement is in PENDING status.
     *
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return status == DisbursementStatus.PENDING;
    }

    /**
     * Checks if the disbursement is in PROCESSING status.
     *
     * @return true if status is PROCESSING
     */
    public boolean isProcessing() {
        return status == DisbursementStatus.PROCESSING;
    }

    /**
     * Checks if the disbursement is in COMPLETED status.
     *
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return status == DisbursementStatus.COMPLETED;
    }

    /**
     * Checks if the disbursement is in FAILED status.
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return status == DisbursementStatus.FAILED;
    }

    /**
     * Checks if the disbursement has reached a terminal state.
     *
     * @return true if status is COMPLETED or FAILED
     */
    public boolean isTerminal() {
        return status == DisbursementStatus.COMPLETED || status == DisbursementStatus.FAILED;
    }

    // ==================== VALIDATION METHODS ====================

    private static void validateSourceAccountId(UUID sourceAccountId) {
        if (sourceAccountId == null) {
            throw new IllegalArgumentException("Source account ID cannot be null");
        }
    }

    private static void validateAmount(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private static void validateBankCode(String bankCode) {
        if (bankCode == null || bankCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank code cannot be null or empty");
        }
    }

    private static void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
    }

    private static void validateAccountName(String accountName) {
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }
    }

    private static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }
    }

    private static String generateIdempotencyKey() {
        return "disb-" + UUID.randomUUID().toString().replace("-", "");
    }
}
