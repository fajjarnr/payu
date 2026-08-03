package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.*;
import id.payu.wallet.domain.port.in.SettlementUseCase;
import id.payu.wallet.domain.port.in.SplitPaymentUseCase;
import id.payu.wallet.domain.port.out.SettlementPersistencePort;
import id.payu.wallet.domain.port.out.JournalPersistencePort;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import id.payu.wallet.domain.model.DiscrepancyType;
import id.payu.wallet.domain.model.EntryStatus;
import id.payu.wallet.domain.model.SettlementStatus;
import id.payu.wallet.domain.model.SplitType;

/**
 * Application service for settlement operations (GAP-003, GAP-013).
 * Orchestrates daily settlement batch processing and revenue sharing.
 */
@Service
public class SettlementService implements SettlementUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SettlementService.class);

    private final SettlementPersistencePort settlementPersistencePort;
    private final JournalPersistencePort journalPersistencePort;
    private final SplitPaymentUseCase splitPaymentUseCase;
    private final java.time.Clock clock;

    public SettlementService(SettlementPersistencePort settlementPersistencePort,
                             JournalPersistencePort journalPersistencePort,
                             SplitPaymentUseCase splitPaymentUseCase,
                             java.time.Clock clock) {
        this.settlementPersistencePort = settlementPersistencePort;
        this.journalPersistencePort = journalPersistencePort;
        this.splitPaymentUseCase = splitPaymentUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SettlementBatch createSettlementBatch(String partnerId, LocalDate settlementDate, String currency) {
        log.info("Creating settlement batch for partner {} on date {}", partnerId, settlementDate);

        SettlementBatch batch = SettlementBatch.create(partnerId, settlementDate, currency);
        SettlementBatch saved = settlementPersistencePort.saveSettlementBatch(batch);

        log.info("Created settlement batch {} for partner {}", saved.getId(), partnerId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementBatch getSettlementBatch(UUID batchId) {
        return settlementPersistencePort.findSettlementBatchById(batchId)
                .orElseThrow(() -> new SettlementNotFoundException(batchId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementBatch> getSettlementBatchesByPartner(String partnerId, LocalDate from, LocalDate to) {
        return settlementPersistencePort.findSettlementBatchesByPartner(partnerId, from, to);
    }

    @Override
    @Transactional
    public SettlementBatch processDailySettlement(String partnerId, LocalDate settlementDate) {
        log.info("Processing daily settlement for partner {} on date {}", partnerId, settlementDate);

        // Check if batch already exists
        List<SettlementBatch> existing = settlementPersistencePort.findSettlementBatchesByPartner(
                partnerId, settlementDate, settlementDate);

        if (!existing.isEmpty()) {
            log.warn("Settlement batch already exists for partner {} on date {}", partnerId, settlementDate);
            return existing.get(0);
        }

        // Create new batch
        SettlementBatch batch = createSettlementBatch(partnerId, settlementDate, "IDR");

        // Populate entries from journal entries for the date
        populateSettlementEntries(batch);

        // Detect discrepancies
        batch = detectDiscrepancies(batch.getId());

        log.info("Completed daily settlement processing for batch {}", batch.getId());
        return batch;
    }

    @Override
    @Transactional
    public SettlementBatch startProcessing(UUID batchId, String processedBy) {
        log.info("Starting processing for settlement batch {} by {}", batchId, processedBy);

        SettlementBatch batch = getSettlementBatch(batchId);
        batch.startProcessing(processedBy);

        SettlementBatch saved = settlementPersistencePort.saveSettlementBatch(batch);
        log.info("Settlement batch {} now in PROCESSING state", batchId);
        return saved;
    }

    @Override
    @Transactional
    public SettlementBatch completeSettlement(UUID batchId) {
        log.info("Completing settlement batch {}", batchId);

        SettlementBatch batch = getSettlementBatch(batchId);
        batch.complete();

        SettlementBatch saved = settlementPersistencePort.saveSettlementBatch(batch);
        log.info("Settlement batch {} COMPLETED", batchId);
        return saved;
    }

    @Override
    @Transactional
    public SettlementBatch failSettlement(UUID batchId, String reason) {
        log.error("Failing settlement batch {}: {}", batchId, reason);

        SettlementBatch batch = getSettlementBatch(batchId);
        batch.fail(reason);

        SettlementBatch saved = settlementPersistencePort.saveSettlementBatch(batch);
        log.info("Settlement batch {} FAILED: {}", batchId, reason);
        return saved;
    }

    @Override
    @Transactional
    public SettlementBatch manualOverride(UUID batchId, String reason, String overriddenBy) {
        log.warn("Manual override for settlement batch {} by {}: {}", batchId, overriddenBy, reason);

        SettlementBatch batch = getSettlementBatch(batchId);
        batch.manualOverride(reason, overriddenBy);

        SettlementBatch saved = settlementPersistencePort.saveSettlementBatch(batch);
        log.info("Settlement batch {} OVERRIDDEN by {}", batchId, overriddenBy);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public String generateReconciliationReport(UUID batchId) {
        SettlementBatch batch = getSettlementBatch(batchId);

        StringBuilder report = new StringBuilder();
        report.append("=== SETTLEMENT RECONCILIATION REPORT ===\n");
        report.append("Batch ID: ").append(batchId).append("\n");
        report.append("Partner ID: ").append(batch.getPartnerId()).append("\n");
        report.append("Settlement Date: ").append(batch.getSettlementDate()).append("\n");
        report.append("Status: ").append(batch.getStatus()).append("\n");
        report.append("Total Amount: ").append(batch.getTotalAmount()).append(" ").append(batch.getCurrency()).append("\n");
        report.append("Fee Amount: ").append(batch.getFeeAmount()).append(" ").append(batch.getCurrency()).append("\n");
        report.append("Net Amount: ").append(batch.getNetAmount()).append(" ").append(batch.getCurrency()).append("\n");
        report.append("Entries: ").append(batch.getEntries().size()).append("\n");

        if (batch.hasDiscrepancies()) {
            report.append("\n=== DISCREPANCIES ===\n");
            for (Discrepancy d : batch.getDiscrepancies()) {
                report.append("- ").append(d.getType()).append(": ").append(d.getDescription()).append("\n");
            }
        }

        report.append("\nGenerated at: ").append(LocalDateTime.now()).append("\n");

        return report.toString();
    }

    @Override
    @Transactional
    public SettlementBatch detectDiscrepancies(UUID batchId) {
        SettlementBatch batch = getSettlementBatch(batchId);

        // Check for amount mismatches
        BigDecimal calculatedTotal = batch.getEntries().stream()
                .map(SettlementEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (calculatedTotal.compareTo(batch.getTotalAmount()) != 0) {
            Discrepancy discrepancy = Discrepancy.create(
                    batchId, null, DiscrepancyType.AMOUNT_MISMATCH,
                    "Total amount mismatch: calculated=" + calculatedTotal + ", recorded=" + batch.getTotalAmount(),
                    batch.getTotalAmount(), calculatedTotal
            );
            batch.addDiscrepancy(discrepancy);
        }

        // Check for failed entries
        long failedCount = batch.getEntries().stream()
                .filter(e -> e.getStatus() == EntryStatus.FAILED)
                .count();

        if (failedCount > 0) {
            Discrepancy discrepancy = Discrepancy.create(
                    batchId, null, DiscrepancyType.OTHER,
                    "Found " + failedCount + " failed settlement entries",
                    BigDecimal.ZERO, new BigDecimal(failedCount)
            );
            batch.addDiscrepancy(discrepancy);
        }

        return settlementPersistencePort.saveSettlementBatch(batch);
    }

    @Override
    @Transactional
    public RevenueSplit createRevenueSplit(String partnerId, String name, String description,
                                            SplitType splitType, String createdBy) {
        log.info("Creating revenue split '{}' for partner {}", name, partnerId);

        RevenueSplit split = RevenueSplit.create(partnerId, name, description, splitType, createdBy);
        RevenueSplit saved = settlementPersistencePort.saveRevenueSplit(split);

        log.info("Created revenue split {} for partner {}", saved.getId(), partnerId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSplit getRevenueSplit(UUID splitId) {
        return settlementPersistencePort.findRevenueSplitById(splitId)
                .orElseThrow(() -> new RevenueSplitNotFoundException(splitId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueSplit> getRevenueSplitsByPartner(String partnerId) {
        return settlementPersistencePort.findRevenueSplitsByPartner(partnerId);
    }

    @Override
    @Transactional
    public RevenueSplit addStakeholder(UUID splitId, String accountId, String name,
                                        BigDecimal percentage, BigDecimal fixedAmount, int priority) {
        log.info("Adding stakeholder {} to revenue split {}", accountId, splitId);

        RevenueSplit split = getRevenueSplit(splitId);
        split.addStakeholder(accountId, name, percentage, fixedAmount, priority);

        RevenueSplit saved = settlementPersistencePort.saveRevenueSplit(split);
        log.info("Added stakeholder to revenue split {}", splitId);
        return saved;
    }

    @Override
    @Transactional
    public RevenueSplit deactivateRevenueSplit(UUID splitId) {
        log.info("Deactivating revenue split {}", splitId);

        RevenueSplit split = getRevenueSplit(splitId);
        split.deactivate();

        RevenueSplit saved = settlementPersistencePort.saveRevenueSplit(split);
        log.info("Deactivated revenue split {}", splitId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalculatedSplit> calculateRevenueSplits(UUID splitId, BigDecimal amount) {
        RevenueSplit split = getRevenueSplit(splitId);
        return split.calculateSplits(amount);
    }

    @Override
    public void applyRevenueSplit(UUID settlementBatchId, UUID revenueSplitId) {
        log.info("Applying revenue split {} to settlement batch {}", revenueSplitId, settlementBatchId);

        SettlementBatch batch = getSettlementBatch(settlementBatchId);
        RevenueSplit split = getRevenueSplit(revenueSplitId);

        List<CalculatedSplit> calculatedSplits = split.calculateSplits(batch.getNetAmount());

        List<SplitRecipient> recipients = calculatedSplits.stream()
                .map(calc -> SplitRecipient.builder()
                        .recipientAccountId(calc.getAccountId())
                        .recipientLabel(calc.getName())
                        .type(RecipientType.MERCHANT)
                        .fixedAmount(calc.getAmount())
                        .build())
                .toList();
        SplitPaymentExecution execution = splitPaymentUseCase.executeAdHocSplit(
                batch.getPartnerId(), batch.getPartnerId(), batch.getNetAmount(), batch.getCurrency(),
                recipients, settlementBatchId.toString(), "Revenue split", "SETTLEMENT:" + settlementBatchId);
        if (execution.getStatus() != SplitExecutionStatus.COMPLETED) {
            throw new IllegalStateException("Revenue split requires reconciliation: " + execution.getStatus());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String generateRoyaltyStatement(String partnerId, String accountId, int year, int month) {
        YearMonth period = YearMonth.of(year, month);
        StringBuilder statement = new StringBuilder();
        statement.append("=== ROYALTY STATEMENT ===\n");
        statement.append("Partner ID: ").append(partnerId).append("\n");
        statement.append("Account ID: ").append(accountId).append("\n");
        statement.append("Period: ").append(period).append("\n");
        statement.append("Generated: ").append(LocalDateTime.now(clock)).append("\n\n");

        // Use persisted settlement batches as the source of settled royalty amounts.
        List<SettlementBatch> batches = settlementPersistencePort.findSettlementBatchesByPartner(
                partnerId, period.atDay(1), period.atEndOfMonth());
        List<RevenueSplit> splits = settlementPersistencePort.findRevenueSplitsByPartner(partnerId);

        BigDecimal totalRoyalties = BigDecimal.ZERO;

        for (RevenueSplit split : splits) {
            for (SettlementBatch batch : batches) {
                if (!isCompletedSettlement(batch)
                        || batch.getSettlementDate() == null
                        || batch.getNetAmount() == null
                        || !split.isEffectiveAt(batch.getSettlementDate().atStartOfDay())) {
                    continue;
                }

                CalculatedSplit calculated = split.calculateSplits(batch.getNetAmount()).stream()
                        .filter(candidate -> accountId.equals(candidate.getAccountId()))
                        .findFirst()
                        .orElse(null);
                if (calculated == null) {
                    continue;
                }

                statement.append("Revenue Split: ").append(split.getName()).append("\n");
                statement.append("  Settlement Date: ").append(batch.getSettlementDate()).append("\n");
                statement.append("  Amount: ").append(calculated.getAmount()).append("\n\n");
                totalRoyalties = totalRoyalties.add(calculated.getAmount());
            }
        }

        statement.append("Total Royalties: ").append(totalRoyalties).append("\n");

        return statement.toString();
    }

    private boolean isCompletedSettlement(SettlementBatch batch) {
        return batch.getStatus() == SettlementStatus.COMPLETED
                || batch.getStatus() == SettlementStatus.OVERRIDDEN;
    }

    /**
     * Scheduled job to process daily settlements (runs at 2 AM daily).
     */
    @SchedulerLock(name = "SettlementService_scheduledDailySettlement", lockAtLeastFor = "PT1S", lockAtMostFor = "PT4H")@Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void scheduledDailySettlement() {
        log.info("Running scheduled daily settlement job");

        LocalDate yesterday = LocalDate.now(clock).minusDays(1);

        // Get all pending batches for yesterday
        List<SettlementBatch> pendingBatches = settlementPersistencePort.findSettlementBatchesByDate(yesterday)
                .stream()
                .filter(b -> b.getStatus() == SettlementStatus.PENDING)
                .collect(Collectors.toList());

        for (SettlementBatch batch : pendingBatches) {
            try {
                startProcessing(batch.getId(), "SYSTEM");

                if (batch.hasDiscrepancies()) {
                    log.warn("Settlement batch {} has discrepancies, failing", batch.getId());
                    failSettlement(batch.getId(), "Discrepancies detected during reconciliation");
                } else {
                    completeSettlement(batch.getId());
                }
            } catch (Exception e) {
                log.error("Error processing settlement batch {}", batch.getId(), e);
                failSettlement(batch.getId(), "Error: " + e.getMessage());
            }
        }

        log.info("Completed scheduled daily settlement job");
    }

    private void populateSettlementEntries(SettlementBatch batch) {
        // Get journal entries for the settlement date
        LocalDateTime from = batch.getSettlementDate().atStartOfDay();
        LocalDateTime to = batch.getSettlementDate().plusDays(1).atStartOfDay();

        List<JournalEntry> journals = journalPersistencePort.findJournalsByPostedAtBetween(from, to);

        for (JournalEntry journal : journals) {
            SettlementEntry entry = SettlementEntry.create(
                    batch.getId(),
                    journal.getId().toString(),
                    journal.getReferenceType(),
                    journal.getReferenceId(),
                    journal.getTotalDebit(),
                    batch.getCurrency(),
                    BigDecimal.ZERO
            );
            batch.addEntry(entry);
        }

        settlementPersistencePort.saveSettlementBatch(batch);
    }
}
