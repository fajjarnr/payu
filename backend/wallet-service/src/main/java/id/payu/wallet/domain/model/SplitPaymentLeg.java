package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single leg (recipient transfer) in a split payment execution.
 */
public class SplitPaymentLeg {

    private UUID id;
    private UUID executionId;
    private String recipientAccountId;
    private String recipientLabel;
    private BigDecimal amount;
    private LegStatus status;
    private UUID journalEntryId;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;

    public SplitPaymentLeg() {
    }

    public enum LegStatus {
        PENDING,
        CREDITED,
        FAILED,
        REVERSED
    }

    public void markCredited(UUID journalEntryId) {
        this.status = LegStatus.CREDITED;
        this.journalEntryId = journalEntryId;
        this.settledAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = LegStatus.FAILED;
    }

    public void markReversed() {
        this.status = LegStatus.REVERSED;
    }

    public static SplitPaymentLegBuilder builder() {
        return new SplitPaymentLegBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }
    public String getRecipientAccountId() { return recipientAccountId; }
    public void setRecipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; }
    public String getRecipientLabel() { return recipientLabel; }
    public void setRecipientLabel(String recipientLabel) { this.recipientLabel = recipientLabel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LegStatus getStatus() { return status; }
    public void setStatus(LegStatus status) { this.status = status; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class SplitPaymentLegBuilder {
        private UUID id;
        private UUID executionId;
        private String recipientAccountId;
        private String recipientLabel;
        private BigDecimal amount;
        private LegStatus status = LegStatus.PENDING;
        private UUID journalEntryId;
        private LocalDateTime settledAt;
        private LocalDateTime createdAt;

        SplitPaymentLegBuilder() {}

        public SplitPaymentLegBuilder id(UUID id) { this.id = id; return this; }
        public SplitPaymentLegBuilder executionId(UUID executionId) { this.executionId = executionId; return this; }
        public SplitPaymentLegBuilder recipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; return this; }
        public SplitPaymentLegBuilder recipientLabel(String recipientLabel) { this.recipientLabel = recipientLabel; return this; }
        public SplitPaymentLegBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public SplitPaymentLegBuilder status(LegStatus status) { this.status = status; return this; }
        public SplitPaymentLegBuilder journalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; return this; }
        public SplitPaymentLegBuilder settledAt(LocalDateTime settledAt) { this.settledAt = settledAt; return this; }
        public SplitPaymentLegBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public SplitPaymentLeg build() {
            SplitPaymentLeg l = new SplitPaymentLeg();
            l.id = this.id;
            l.executionId = this.executionId;
            l.recipientAccountId = this.recipientAccountId;
            l.recipientLabel = this.recipientLabel;
            l.amount = this.amount;
            l.status = this.status;
            l.journalEntryId = this.journalEntryId;
            l.settledAt = this.settledAt;
            l.createdAt = this.createdAt;
            return l;
        }
    }
}
