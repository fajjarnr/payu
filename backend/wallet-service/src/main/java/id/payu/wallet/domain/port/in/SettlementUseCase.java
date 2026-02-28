package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.SettlementBatch;
import id.payu.wallet.domain.model.RevenueSplit;
import id.payu.wallet.domain.model.CalculatedSplit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for settlement operations (GAP-003, GAP-013).
 * Defines use cases for daily settlement and revenue sharing.
 */
public interface SettlementUseCase {

    /**
     * Create a new settlement batch for a partner.
     */
    SettlementBatch createSettlementBatch(String partnerId, LocalDate settlementDate, String currency);

    /**
     * Get settlement batch by ID.
     */
    SettlementBatch getSettlementBatch(UUID batchId);

    /**
     * Get settlement batches for a partner.
     */
    List<SettlementBatch> getSettlementBatchesByPartner(String partnerId, LocalDate from, LocalDate to);

    /**
     * Process daily settlement batch (scheduled job).
     */
    SettlementBatch processDailySettlement(String partnerId, LocalDate settlementDate);

    /**
     * Start processing a settlement batch.
     */
    SettlementBatch startProcessing(UUID batchId, String processedBy);

    /**
     * Complete a settlement batch.
     */
    SettlementBatch completeSettlement(UUID batchId);

    /**
     * Fail a settlement batch with reason.
     */
    SettlementBatch failSettlement(UUID batchId, String reason);

    /**
     * Manual override for failed settlement (admin action).
     */
    SettlementBatch manualOverride(UUID batchId, String reason, String overriddenBy);

    /**
     * Generate reconciliation report for a settlement batch.
     */
    String generateReconciliationReport(UUID batchId);

    /**
     * Detect discrepancies in a settlement batch.
     */
    SettlementBatch detectDiscrepancies(UUID batchId);

    /**
     * Create a revenue split configuration.
     */
    RevenueSplit createRevenueSplit(String partnerId, String name, String description,
                                     RevenueSplit.SplitType splitType, String createdBy);

    /**
     * Get revenue split by ID.
     */
    RevenueSplit getRevenueSplit(UUID splitId);

    /**
     * Get active revenue splits for a partner.
     */
    List<RevenueSplit> getRevenueSplitsByPartner(String partnerId);

    /**
     * Add stakeholder to revenue split.
     */
    RevenueSplit addStakeholder(UUID splitId, String accountId, String name,
                                 BigDecimal percentage, BigDecimal fixedAmount, int priority);

    /**
     * Deactivate a revenue split.
     */
    RevenueSplit deactivateRevenueSplit(UUID splitId);

    /**
     * Calculate revenue splits for an amount.
     */
    List<CalculatedSplit> calculateRevenueSplits(UUID splitId, BigDecimal amount);

    /**
     * Apply revenue split during settlement.
     */
    void applyRevenueSplit(UUID settlementBatchId, UUID revenueSplitId);

    /**
     * Generate royalty statement for a stakeholder (monthly).
     */
    String generateRoyaltyStatement(String partnerId, String accountId, int year, int month);
}
