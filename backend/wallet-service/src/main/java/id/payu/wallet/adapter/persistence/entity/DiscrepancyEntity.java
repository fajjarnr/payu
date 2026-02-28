package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for settlement discrepancies found during reconciliation.
 */
@Entity
@Table(name = "settlement_discrepancies", indexes = {
    @Index(name = "idx_discrepancy_batch", columnList = "settlement_batch_id"),
    @Index(name = "idx_discrepancy_resolved", columnList = "resolved")
})
public class DiscrepancyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_batch_id", nullable = false)
    private SettlementBatchEntity settlementBatch;

    @Column(name = "transaction_id", length = 128)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscrepancyType type;

    @Column(length = 512)
    private String description;

    @Column(name = "expected_amount", precision = 19, scale = 4)
    private BigDecimal expectedAmount;

    @Column(name = "actual_amount", precision = 19, scale = 4)
    private BigDecimal actualAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal difference;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public DiscrepancyEntity() {
    }

    public enum DiscrepancyType {
        AMOUNT_MISMATCH,
        MISSING_TRANSACTION,
        DUPLICATE_TRANSACTION,
        CURRENCY_MISMATCH,
        TIMING_DIFFERENCE,
        OTHER
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SettlementBatchEntity getSettlementBatch() { return settlementBatch; }
    public void setSettlementBatch(SettlementBatchEntity settlementBatch) { this.settlementBatch = settlementBatch; }
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
}
