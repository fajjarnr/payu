package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Value object representing a single settlement entry within a batch.
 * Immutable after creation.
 */
public class SettlementEntry {

    private UUID id;
    private UUID settlementBatchId;
    private String transactionId;
    private String referenceType;
    private String referenceId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private EntryStatus status;
    private LocalDateTime createdAt;

    public SettlementEntry() {
    }

    public SettlementEntry(UUID id, UUID settlementBatchId, String transactionId,
                           String referenceType, String referenceId, BigDecimal amount,
                           String currency, BigDecimal fee, BigDecimal netAmount,
                           EntryStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.settlementBatchId = settlementBatchId;
        this.transactionId = transactionId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.amount = amount;
        this.currency = currency;
        this.fee = fee;
        this.netAmount = netAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Create a new settlement entry.
     */
    public static SettlementEntry create(UUID settlementBatchId, String transactionId,
                                          String referenceType, String referenceId,
                                          BigDecimal amount, String currency, BigDecimal fee) {
        SettlementEntry entry = new SettlementEntry();
        entry.id = UUID.randomUUID();
        entry.settlementBatchId = settlementBatchId;
        entry.transactionId = transactionId;
        entry.referenceType = referenceType;
        entry.referenceId = referenceId;
        entry.amount = amount;
        entry.currency = currency;
        entry.fee = fee != null ? fee : BigDecimal.ZERO;
        entry.netAmount = amount.subtract(entry.fee);
        entry.status = EntryStatus.PENDING;
        entry.createdAt = LocalDateTime.now();
        return entry;
    }

    public void markSettled() {
        this.status = EntryStatus.SETTLED;
    }

    public void markFailed(String reason) {
        this.status = EntryStatus.FAILED;
    }

    public static SettlementEntryBuilder builder() {
        return new SettlementEntryBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSettlementBatchId() { return settlementBatchId; }
    public void setSettlementBatchId(UUID settlementBatchId) { this.settlementBatchId = settlementBatchId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public EntryStatus getStatus() { return status; }
    public void setStatus(EntryStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class SettlementEntryBuilder {
        private UUID id;
        private UUID settlementBatchId;
        private String transactionId;
        private String referenceType;
        private String referenceId;
        private BigDecimal amount;
        private String currency;
        private BigDecimal fee;
        private BigDecimal netAmount;
        private EntryStatus status;
        private LocalDateTime createdAt;

        SettlementEntryBuilder() {}

        public SettlementEntryBuilder id(UUID id) { this.id = id; return this; }
        public SettlementEntryBuilder settlementBatchId(UUID settlementBatchId) { this.settlementBatchId = settlementBatchId; return this; }
        public SettlementEntryBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public SettlementEntryBuilder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public SettlementEntryBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public SettlementEntryBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public SettlementEntryBuilder currency(String currency) { this.currency = currency; return this; }
        public SettlementEntryBuilder fee(BigDecimal fee) { this.fee = fee; return this; }
        public SettlementEntryBuilder netAmount(BigDecimal netAmount) { this.netAmount = netAmount; return this; }
        public SettlementEntryBuilder status(EntryStatus status) { this.status = status; return this; }
        public SettlementEntryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public SettlementEntry build() {
            return new SettlementEntry(id, settlementBatchId, transactionId, referenceType,
                    referenceId, amount, currency, fee, netAmount, status, createdAt);
        }
    }
}
