package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SettlementBatch aggregate root for daily settlement processing (GAP-003).
 * Manages the lifecycle of partner settlements: PENDING → PROCESSING → COMPLETED/FAILED.
 */
public class SettlementBatch {

    private UUID id;
    private String partnerId;
    private LocalDate settlementDate;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private SettlementStatus status;
    private List<SettlementEntry> entries;
    private String reconciliationReport;
    private List<Discrepancy> discrepancies;
    private String failureReason;
    private String processedBy;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tenantId;

    public SettlementBatch() {
        this.entries = new ArrayList<>();
        this.discrepancies = new ArrayList<>();
    }

    public SettlementBatch(UUID id, String partnerId, LocalDate settlementDate, String currency,
                           BigDecimal totalAmount, BigDecimal feeAmount, BigDecimal netAmount,
                           SettlementStatus status, List<SettlementEntry> entries,
                           String reconciliationReport, List<Discrepancy> discrepancies,
                           String failureReason, String processedBy, LocalDateTime processedAt,
                           LocalDateTime createdAt, LocalDateTime updatedAt, String tenantId) {
        this.id = id;
        this.partnerId = partnerId;
        this.settlementDate = settlementDate;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.status = status;
        this.entries = entries != null ? entries : new ArrayList<>();
        this.reconciliationReport = reconciliationReport;
        this.discrepancies = discrepancies != null ? discrepancies : new ArrayList<>();
        this.failureReason = failureReason;
        this.processedBy = processedBy;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tenantId = tenantId;
    }

    /**
     * Create a new settlement batch for a partner.
     */
    public static SettlementBatch create(String partnerId, LocalDate settlementDate, String currency) {
        SettlementBatch batch = new SettlementBatch();
        batch.id = UUID.randomUUID();
        batch.partnerId = partnerId;
        batch.settlementDate = settlementDate;
        batch.currency = currency != null ? currency : "IDR";
        batch.totalAmount = BigDecimal.ZERO;
        batch.feeAmount = BigDecimal.ZERO;
        batch.netAmount = BigDecimal.ZERO;
        batch.status = SettlementStatus.PENDING;
        batch.entries = new ArrayList<>();
        batch.discrepancies = new ArrayList<>();
        batch.createdAt = LocalDateTime.now();
        batch.updatedAt = LocalDateTime.now();
        return batch;
    }

    /**
     * Add a settlement entry to this batch.
     */
    public void addEntry(SettlementEntry entry) {
        entries.add(entry);
        recalculateTotals();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Start processing the settlement batch.
     */
    public void startProcessing(String processedBy) {
        if (status != SettlementStatus.PENDING) {
            throw new IllegalStateException("Cannot start processing: batch is " + status);
        }
        this.status = SettlementStatus.PROCESSING;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark the settlement as completed.
     */
    public void complete() {
        if (status != SettlementStatus.PROCESSING) {
            throw new IllegalStateException("Cannot complete: batch is " + status);
        }
        this.status = SettlementStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark the settlement as failed with a reason.
     */
    public void fail(String reason) {
        if (status != SettlementStatus.PROCESSING && status != SettlementStatus.PENDING) {
            throw new IllegalStateException("Cannot fail: batch is " + status);
        }
        this.status = SettlementStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Add a discrepancy found during reconciliation.
     */
    public void addDiscrepancy(Discrepancy discrepancy) {
        discrepancies.add(discrepancy);
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if batch has any discrepancies.
     */
    public boolean hasDiscrepancies() {
        return !discrepancies.isEmpty();
    }

    /**
     * Manual override for settlement exceptions (admin action).
     */
    public void manualOverride(String reason, String overriddenBy) {
        if (status != SettlementStatus.FAILED) {
            throw new IllegalStateException("Can only override failed settlements");
        }
        this.status = SettlementStatus.COMPLETED;
        this.failureReason = "OVERRIDE: " + reason + " by " + overriddenBy;
        this.updatedAt = LocalDateTime.now();
    }

    private void recalculateTotals() {
        this.totalAmount = entries.stream()
                .map(SettlementEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.netAmount = this.totalAmount.subtract(this.feeAmount != null ? this.feeAmount : BigDecimal.ZERO);
    }

    public enum SettlementStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        OVERRIDDEN
    }

    public static SettlementBatchBuilder builder() {
        return new SettlementBatchBuilder();
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
    public List<SettlementEntry> getEntries() { return entries; }
    public void setEntries(List<SettlementEntry> entries) { this.entries = entries; }
    public String getReconciliationReport() { return reconciliationReport; }
    public void setReconciliationReport(String reconciliationReport) { this.reconciliationReport = reconciliationReport; }
    public List<Discrepancy> getDiscrepancies() { return discrepancies; }
    public void setDiscrepancies(List<Discrepancy> discrepancies) { this.discrepancies = discrepancies; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public static class SettlementBatchBuilder {
        private UUID id;
        private String partnerId;
        private LocalDate settlementDate;
        private String currency;
        private BigDecimal totalAmount;
        private BigDecimal feeAmount;
        private BigDecimal netAmount;
        private SettlementStatus status;
        private List<SettlementEntry> entries;
        private String reconciliationReport;
        private List<Discrepancy> discrepancies;
        private String failureReason;
        private String processedBy;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String tenantId;

        SettlementBatchBuilder() {}

        public SettlementBatchBuilder id(UUID id) { this.id = id; return this; }
        public SettlementBatchBuilder partnerId(String partnerId) { this.partnerId = partnerId; return this; }
        public SettlementBatchBuilder settlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; return this; }
        public SettlementBatchBuilder currency(String currency) { this.currency = currency; return this; }
        public SettlementBatchBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public SettlementBatchBuilder feeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; return this; }
        public SettlementBatchBuilder netAmount(BigDecimal netAmount) { this.netAmount = netAmount; return this; }
        public SettlementBatchBuilder status(SettlementStatus status) { this.status = status; return this; }
        public SettlementBatchBuilder entries(List<SettlementEntry> entries) { this.entries = entries; return this; }
        public SettlementBatchBuilder reconciliationReport(String reconciliationReport) { this.reconciliationReport = reconciliationReport; return this; }
        public SettlementBatchBuilder discrepancies(List<Discrepancy> discrepancies) { this.discrepancies = discrepancies; return this; }
        public SettlementBatchBuilder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public SettlementBatchBuilder processedBy(String processedBy) { this.processedBy = processedBy; return this; }
        public SettlementBatchBuilder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
        public SettlementBatchBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public SettlementBatchBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public SettlementBatchBuilder tenantId(String tenantId) { this.tenantId = tenantId; return this; }

        public SettlementBatch build() {
            return new SettlementBatch(id, partnerId, settlementDate, currency, totalAmount, feeAmount,
                    netAmount, status, entries, reconciliationReport, discrepancies, failureReason,
                    processedBy, processedAt, createdAt, updatedAt, tenantId);
        }
    }
}
