package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.EntityListeners;

import id.payu.wallet.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for settlement batches (GAP-003).
 * Persists daily settlement state for partner reconciliation.
 */
@Entity
@Table(name = "settlement_batches", indexes = {
    @Index(name = "idx_settlement_partner", columnList = "partner_id"),
    @Index(name = "idx_settlement_date", columnList = "settlement_date"),
    @Index(name = "idx_settlement_status", columnList = "status"),
    @Index(name = "idx_settlement_partner_date", columnList = "partner_id, settlement_date"),
    @Index(name = "idx_settlement_tenant", columnList = "tenant_id")
})
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class SettlementBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "partner_id", nullable = false, length = 128)
    private String partnerId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "fee_amount", precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementStatus status;

    @OneToMany(mappedBy = "settlementBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SettlementEntryEntity> entries = new ArrayList<>();

    @Column(name = "reconciliation_report", length = 4000)
    private String reconciliationReport;

    @OneToMany(mappedBy = "settlementBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiscrepancyEntity> discrepancies = new ArrayList<>();

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SettlementBatchEntity() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public List<SettlementEntryEntity> getEntries() { return entries; }
    public void setEntries(List<SettlementEntryEntity> entries) { this.entries = entries; }
    public String getReconciliationReport() { return reconciliationReport; }
    public void setReconciliationReport(String reconciliationReport) { this.reconciliationReport = reconciliationReport; }
    public List<DiscrepancyEntity> getDiscrepancies() { return discrepancies; }
    public void setDiscrepancies(List<DiscrepancyEntity> discrepancies) { this.discrepancies = discrepancies; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
