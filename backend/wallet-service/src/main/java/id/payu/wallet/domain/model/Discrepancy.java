package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Value object representing a discrepancy found during reconciliation.
 */
public class Discrepancy {

    private UUID id;
    private UUID settlementBatchId;
    private String transactionId;
    private DiscrepancyType type;
    private String description;
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private BigDecimal difference;
    private boolean resolved;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;

    public Discrepancy() {
    }

    public Discrepancy(UUID id, UUID settlementBatchId, String transactionId,
                       DiscrepancyType type, String description, BigDecimal expectedAmount,
                       BigDecimal actualAmount, BigDecimal difference, boolean resolved,
                       String resolvedBy, LocalDateTime resolvedAt, LocalDateTime createdAt) {
        this.id = id;
        this.settlementBatchId = settlementBatchId;
        this.transactionId = transactionId;
        this.type = type;
        this.description = description;
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;
        this.difference = difference;
        this.resolved = resolved;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
    }

    /**
     * Create a new discrepancy.
     */
    public static Discrepancy create(UUID settlementBatchId, String transactionId,
                                      DiscrepancyType type, String description,
                                      BigDecimal expectedAmount, BigDecimal actualAmount) {
        Discrepancy d = new Discrepancy();
        d.id = UUID.randomUUID();
        d.settlementBatchId = settlementBatchId;
        d.transactionId = transactionId;
        d.type = type;
        d.description = description;
        d.expectedAmount = expectedAmount;
        d.actualAmount = actualAmount;
        d.difference = actualAmount.subtract(expectedAmount);
        d.resolved = false;
        d.createdAt = LocalDateTime.now();
        return d;
    }

    public void resolve(String resolvedBy) {
        this.resolved = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
    }

    public enum DiscrepancyType {
        AMOUNT_MISMATCH,
        MISSING_TRANSACTION,
        DUPLICATE_TRANSACTION,
        CURRENCY_MISMATCH,
        TIMING_DIFFERENCE,
        OTHER
    }

    public static DiscrepancyBuilder builder() {
        return new DiscrepancyBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSettlementBatchId() { return settlementBatchId; }
    public void setSettlementBatchId(UUID settlementBatchId) { this.settlementBatchId = settlementBatchId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public DiscrepancyType getType() { return type; }
    public void setType(DiscrepancyType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class DiscrepancyBuilder {
        private UUID id;
        private UUID settlementBatchId;
        private String transactionId;
        private DiscrepancyType type;
        private String description;
        private BigDecimal expectedAmount;
        private BigDecimal actualAmount;
        private BigDecimal difference;
        private boolean resolved;
        private String resolvedBy;
        private LocalDateTime resolvedAt;
        private LocalDateTime createdAt;

        DiscrepancyBuilder() {}

        public DiscrepancyBuilder id(UUID id) { this.id = id; return this; }
        public DiscrepancyBuilder settlementBatchId(UUID settlementBatchId) { this.settlementBatchId = settlementBatchId; return this; }
        public DiscrepancyBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public DiscrepancyBuilder type(DiscrepancyType type) { this.type = type; return this; }
        public DiscrepancyBuilder description(String description) { this.description = description; return this; }
        public DiscrepancyBuilder expectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; return this; }
        public DiscrepancyBuilder actualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; return this; }
        public DiscrepancyBuilder difference(BigDecimal difference) { this.difference = difference; return this; }
        public DiscrepancyBuilder resolved(boolean resolved) { this.resolved = resolved; return this; }
        public DiscrepancyBuilder resolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; return this; }
        public DiscrepancyBuilder resolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; return this; }
        public DiscrepancyBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Discrepancy build() {
            return new Discrepancy(id, settlementBatchId, transactionId, type, description,
                    expectedAmount, actualAmount, difference, resolved, resolvedBy, resolvedAt, createdAt);
        }
    }
}
