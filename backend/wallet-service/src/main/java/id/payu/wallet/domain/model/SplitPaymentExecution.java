package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a specific execution of a split payment — one checkout → one execution.
 * <p>
 * Lifecycle:
 * <pre>
 *   PENDING → PROCESSING → COMPLETED   (happy path — all legs credited)
 *                        → FAILED       (no legs credited, compensation done)
 *           COMPLETED    → REVERSED     (full reversal)
 * </pre>
 * <p>
 * Accounting: one balanced journal per execution.
 * DR Payer Wallet (1100) for totalAmount,
 * CR each recipient wallet (1100) for their leg amount.
 */
public class SplitPaymentExecution {

    private UUID id;
    private UUID splitRuleId;
    private String payerAccountId;
    private String partnerId;
    private BigDecimal totalAmount;
    private String currency;
    private String externalReferenceId;
    private String idempotencyKey;
    private SplitExecutionStatus status;
    private String description;
    private List<SplitPaymentLeg> legs;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private String failureReason;

    public SplitPaymentExecution() {
        this.legs = new ArrayList<>();
    }

    public enum SplitExecutionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REVERSED
    }

    // --- Domain Methods ---

    public void startProcessing() {
        if (this.status != SplitExecutionStatus.PENDING) {
            throw new IllegalStateException("Can only start processing from PENDING, current: " + status);
        }
        this.status = SplitExecutionStatus.PROCESSING;
    }

    public void complete() {
        if (this.status != SplitExecutionStatus.PROCESSING) {
            throw new IllegalStateException("Can only complete from PROCESSING, current: " + status);
        }
        this.status = SplitExecutionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = SplitExecutionStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
    }

    public void reverse() {
        if (this.status != SplitExecutionStatus.COMPLETED) {
            throw new IllegalStateException("Can only reverse from COMPLETED, current: " + status);
        }
        this.status = SplitExecutionStatus.REVERSED;
    }

    /**
     * Verify that total leg amounts equal the total amount.
     */
    public boolean isBalanced() {
        if (legs == null || legs.isEmpty()) return false;
        BigDecimal legTotal = legs.stream()
                .map(SplitPaymentLeg::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return legTotal.compareTo(totalAmount) == 0;
    }

    public static SplitPaymentExecutionBuilder builder() {
        return new SplitPaymentExecutionBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSplitRuleId() { return splitRuleId; }
    public void setSplitRuleId(UUID splitRuleId) { this.splitRuleId = splitRuleId; }
    public String getPayerAccountId() { return payerAccountId; }
    public void setPayerAccountId(String payerAccountId) { this.payerAccountId = payerAccountId; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getExternalReferenceId() { return externalReferenceId; }
    public void setExternalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public SplitExecutionStatus getStatus() { return status; }
    public void setStatus(SplitExecutionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<SplitPaymentLeg> getLegs() { return legs; }
    public void setLegs(List<SplitPaymentLeg> legs) { this.legs = legs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public static class SplitPaymentExecutionBuilder {
        private UUID id;
        private UUID splitRuleId;
        private String payerAccountId;
        private String partnerId;
        private BigDecimal totalAmount;
        private String currency;
        private String externalReferenceId;
        private String idempotencyKey;
        private SplitExecutionStatus status = SplitExecutionStatus.PENDING;
        private String description;
        private List<SplitPaymentLeg> legs = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private LocalDateTime failedAt;
        private String failureReason;

        SplitPaymentExecutionBuilder() {}

        public SplitPaymentExecutionBuilder id(UUID id) { this.id = id; return this; }
        public SplitPaymentExecutionBuilder splitRuleId(UUID splitRuleId) { this.splitRuleId = splitRuleId; return this; }
        public SplitPaymentExecutionBuilder payerAccountId(String payerAccountId) { this.payerAccountId = payerAccountId; return this; }
        public SplitPaymentExecutionBuilder partnerId(String partnerId) { this.partnerId = partnerId; return this; }
        public SplitPaymentExecutionBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public SplitPaymentExecutionBuilder currency(String currency) { this.currency = currency; return this; }
        public SplitPaymentExecutionBuilder externalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; return this; }
        public SplitPaymentExecutionBuilder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public SplitPaymentExecutionBuilder status(SplitExecutionStatus status) { this.status = status; return this; }
        public SplitPaymentExecutionBuilder description(String description) { this.description = description; return this; }
        public SplitPaymentExecutionBuilder legs(List<SplitPaymentLeg> legs) { this.legs = legs; return this; }
        public SplitPaymentExecutionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public SplitPaymentExecutionBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public SplitPaymentExecutionBuilder failedAt(LocalDateTime failedAt) { this.failedAt = failedAt; return this; }
        public SplitPaymentExecutionBuilder failureReason(String failureReason) { this.failureReason = failureReason; return this; }

        public SplitPaymentExecution build() {
            SplitPaymentExecution e = new SplitPaymentExecution();
            e.id = this.id;
            e.splitRuleId = this.splitRuleId;
            e.payerAccountId = this.payerAccountId;
            e.partnerId = this.partnerId;
            e.totalAmount = this.totalAmount;
            e.currency = this.currency;
            e.externalReferenceId = this.externalReferenceId;
            e.idempotencyKey = this.idempotencyKey;
            e.status = this.status;
            e.description = this.description;
            e.legs = this.legs;
            e.createdAt = this.createdAt;
            e.completedAt = this.completedAt;
            e.failedAt = this.failedAt;
            e.failureReason = this.failureReason;
            return e;
        }
    }
}
