package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.*;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.in.SplitPaymentUseCase;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.SplitPaymentPersistencePort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.LegStatus;
import id.payu.wallet.domain.model.SplitExecutionStatus;
import id.payu.wallet.domain.model.SplitType;

/**
 * Application service for split payment operations.
 * <p>
 * Orchestrates multi-recipient payment splitting with:
 * - Atomic wallet debit/credit within a single DB transaction
 * - Double-entry journal for each execution
 * - Idempotency via unique key
 * <p>
 * CoA: DR Payer Wallet (1100) / CR each Recipient Wallet (1100).
 */
@Service
public class SplitPaymentService implements SplitPaymentUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SplitPaymentService.class);

    private static final String COA_USER_WALLETS = "1100";
    private static final String REFERENCE_TYPE = "SPLIT_PAYMENT";

    private final SplitPaymentPersistencePort persistencePort;
    private final WalletUseCase walletUseCase;
    private final JournalUseCase journalUseCase;

    public SplitPaymentService(SplitPaymentPersistencePort persistencePort,
                                WalletUseCase walletUseCase,
                                JournalUseCase journalUseCase) {
        this.persistencePort = persistencePort;
        this.walletUseCase = walletUseCase;
        this.journalUseCase = journalUseCase;
    }

    // --- Rule Management ---

    @Override
    @Transactional
    public SplitPaymentRule createRule(String partnerId, String ruleName,
                                       SplitType splitType, String currency,
                                       List<SplitRecipient> recipients) {
        log.info("Creating split rule: partner={}, name={}, type={}", partnerId, ruleName, splitType);

        SplitPaymentRule rule = SplitPaymentRule.builder()
                .id(UUID.randomUUID())
                .partnerId(partnerId)
                .ruleName(ruleName)
                .splitType(splitType)
                .currency(currency != null ? currency : "IDR")
                .active(true)
                .recipients(recipients)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Validate rule consistency (percentages, etc.)
        rule.validate();

        // Set splitRuleId on all recipients
        for (SplitRecipient r : recipients) {
            r.setSplitRuleId(rule.getId());
            if (r.getId() == null) r.setId(UUID.randomUUID());
        }

        SplitPaymentRule saved = persistencePort.saveRule(rule);
        log.info("Split rule created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public SplitPaymentRule getRule(UUID ruleId) {
        return persistencePort.findRuleById(ruleId)
                .orElseThrow(() -> new SplitPaymentNotFoundException("SplitPaymentRule", ruleId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SplitPaymentRule> getRulesByPartner(String partnerId) {
        return persistencePort.findRulesByPartnerId(partnerId);
    }

    @Override
    @Transactional
    public void deactivateRule(UUID ruleId) {
        SplitPaymentRule rule = getRule(ruleId);
        rule.deactivate();
        persistencePort.saveRule(rule);
        log.info("Split rule deactivated: id={}", ruleId);
    }

    // --- Execution ---

    @Override
    public SplitPaymentExecution executeSplit(UUID ruleId, String payerAccountId,
                                              BigDecimal totalAmount,
                                              String externalReferenceId,
                                              String description,
                                              String idempotencyKey) {
        // Idempotency check
        if (idempotencyKey != null) {
            Optional<SplitPaymentExecution> existing =
                    persistencePort.findExecutionByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Returning existing execution for idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        SplitPaymentRule rule = getRule(ruleId);
        if (!rule.isActive()) {
            throw new IllegalStateException("Split rule is not active: " + ruleId);
        }

        return doExecuteSplit(rule.getId(), rule.getPartnerId(), payerAccountId,
                totalAmount, rule.getCurrency(), rule.computeAmounts(totalAmount),
                externalReferenceId, description, idempotencyKey);
    }

    @Override
    public SplitPaymentExecution executeAdHocSplit(String payerAccountId, String partnerId,
                                                    BigDecimal totalAmount, String currency,
                                                    List<SplitRecipient> recipients,
                                                    String externalReferenceId,
                                                    String description,
                                                    String idempotencyKey) {
        // Idempotency check
        if (idempotencyKey != null) {
            Optional<SplitPaymentExecution> existing =
                    persistencePort.findExecutionByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Returning existing execution for idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        // Build a temporary rule for computation
        SplitPaymentRule tempRule = SplitPaymentRule.builder()
                .splitType(SplitType.FIXED)
                .recipients(recipients)
                .build();

        List<SplitPaymentRule.SplitLegAmount> legAmounts = tempRule.computeAmounts(totalAmount);

        return doExecuteSplit(null, partnerId, payerAccountId,
                totalAmount, currency != null ? currency : "IDR", legAmounts,
                externalReferenceId, description, idempotencyKey);
    }

    private SplitPaymentExecution doExecuteSplit(UUID ruleId, String partnerId,
                                                  String payerAccountId,
                                                  BigDecimal totalAmount, String currency,
                                                  List<SplitPaymentRule.SplitLegAmount> legAmounts,
                                                  String externalReferenceId,
                                                  String description,
                                                  String idempotencyKey) {
        log.info("Executing split payment: payer={}, total={} {}, legs={}",
                maskId(payerAccountId), totalAmount, currency, legAmounts.size());

        // 1. Create execution
        UUID executionId = UUID.randomUUID();
        List<SplitPaymentLeg> legs = new ArrayList<>();
        for (SplitPaymentRule.SplitLegAmount la : legAmounts) {
            legs.add(SplitPaymentLeg.builder()
                    .id(UUID.randomUUID())
                    .executionId(executionId)
                    .recipientAccountId(la.recipient.getRecipientAccountId())
                    .recipientLabel(la.recipient.getRecipientLabel())
                    .amount(la.amount)
                    .status(LegStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        SplitPaymentExecution execution = SplitPaymentExecution.builder()
                .id(executionId)
                .splitRuleId(ruleId)
                .payerAccountId(payerAccountId)
                .partnerId(partnerId)
                .totalAmount(totalAmount)
                .currency(currency)
                .externalReferenceId(externalReferenceId)
                .idempotencyKey(idempotencyKey)
                .status(SplitExecutionStatus.PENDING)
                .description(description)
                .legs(legs)
                .createdAt(LocalDateTime.now())
                .build();

        // Persist before any wallet side effect; this is the saga checkpoint.
        execution.startProcessing();
        persistencePort.saveExecution(execution);
        SplitPaymentExecution saved = reconcileExecution(execution);
        log.info("Split payment execution: id={}, status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Scheduled(fixedDelayString = "${payu.split-payment.reconcile-delay:60000}")
    @SchedulerLock(name = "SplitPaymentService_reconcile", lockAtMostFor = "PT30S")
    public void reconcile() {
        persistencePort.findExecutionsByStatusIn(List.of(
                        SplitExecutionStatus.PROCESSING,
                        SplitExecutionStatus.RECONCILIATION_REQUIRED))
                .forEach(this::reconcileExecution);
    }

    private SplitPaymentExecution reconcileExecution(SplitPaymentExecution execution) {
        if (execution.getStatus() == SplitExecutionStatus.COMPLETED) {
            return execution;
        }
        execution.setStatus(SplitExecutionStatus.PROCESSING);
        for (SplitPaymentLeg leg : execution.getLegs()) {
            if (leg.getStatus() == LegStatus.CREDITED) {
                continue;
            }
            try {
                walletUseCase.transfer(
                        execution.getPayerAccountId(),
                        leg.getRecipientAccountId(),
                        leg.getAmount(),
                        execution.getCurrency(),
                        transferReference(execution, leg),
                        "Split payment: " + (leg.getRecipientLabel() != null ? leg.getRecipientLabel() : "recipient"));
                leg.markCredited(null);
                persistencePort.saveExecution(execution);
            } catch (Exception e) {
                leg.markFailed();
                execution.reconciliationRequired(e.getMessage());
                return persistencePort.saveExecution(execution);
            }
        }

        try {
            createSplitJournal(execution);
            execution.complete();
        } catch (Exception e) {
            execution.reconciliationRequired(e.getMessage());
        }
        return persistencePort.saveExecution(execution);
    }

    private String transferReference(SplitPaymentExecution execution, SplitPaymentLeg leg) {
        return "SPLIT_TRANSFER:" + execution.getId() + ":" + leg.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public SplitPaymentExecution getExecution(UUID executionId) {
        return persistencePort.findExecutionById(executionId)
                .orElseThrow(() -> new SplitPaymentNotFoundException(
                        "SplitPaymentExecution", executionId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SplitPaymentExecution> getExecutionsByPayer(String payerAccountId) {
        return persistencePort.findExecutionsByPayerAccountId(payerAccountId);
    }

    @Override
    @Transactional
    public SplitPaymentExecution reverseExecution(UUID executionId, String reason) {
        log.info("Reversing split payment: id={}, reason={}", executionId, reason);

        SplitPaymentExecution execution = getExecution(executionId);
        execution.reverse();

        // Debit each credited recipient back before crediting payer
        for (SplitPaymentLeg leg : execution.getLegs()) {
            if (leg.getStatus() == LegStatus.CREDITED) {
                // Reserve and commit debit from recipient
                String resId = walletUseCase.reserveBalance(
                        leg.getRecipientAccountId(),
                        leg.getAmount(),
                        executionId.toString());
                walletUseCase.commitReservation(resId);
                leg.markReversed();
            }
        }

        // Credit payer the full amount back
        walletUseCase.credit(
                execution.getPayerAccountId(),
                execution.getTotalAmount(),
                executionId.toString(),
                "Split payment reversal: " + reason);

        // Reversal journal
        createReversalJournal(execution, reason);

        SplitPaymentExecution saved = persistencePort.saveExecution(execution);
        log.info("Split payment reversed: id={}", executionId);
        return saved;
    }

    // --- Journal Helpers ---

    private void createSplitJournal(SplitPaymentExecution execution) {
        if (!journalUseCase.getJournalsByReference(REFERENCE_TYPE, execution.getId().toString()).isEmpty()) {
            return;
        }
        List<LedgerEntry> entries = new ArrayList<>();

        // Debit payer
        entries.add(LedgerEntry.builder()
                .id(UUID.randomUUID())
                .accountId(execution.getPayerAccountId())
                .coaCode(COA_USER_WALLETS)
                .entryType(EntryType.DEBIT)
                .amount(execution.getTotalAmount())
                .currency(execution.getCurrency())
                .referenceType(REFERENCE_TYPE)
                .referenceId(execution.getId().toString())
                .createdAt(LocalDateTime.now())
                .build());

        // Credit each recipient
        for (SplitPaymentLeg leg : execution.getLegs()) {
            entries.add(LedgerEntry.builder()
                    .id(UUID.randomUUID())
                    .accountId(leg.getRecipientAccountId())
                    .coaCode(COA_USER_WALLETS)
                    .entryType(EntryType.CREDIT)
                    .amount(leg.getAmount())
                    .currency(execution.getCurrency())
                    .referenceType(REFERENCE_TYPE)
                    .referenceId(execution.getId().toString())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        journalUseCase.createAndPostJournal(
                "Split payment: " + execution.getDescription(),
                REFERENCE_TYPE,
                execution.getId().toString(),
                entries,
                "split-payment-service");
    }

    private void createReversalJournal(SplitPaymentExecution execution, String reason) {
        List<LedgerEntry> entries = new ArrayList<>();

        // Reverse: credit payer
        entries.add(LedgerEntry.builder()
                .id(UUID.randomUUID())
                .accountId(execution.getPayerAccountId())
                .coaCode(COA_USER_WALLETS)
                .entryType(EntryType.CREDIT)
                .amount(execution.getTotalAmount())
                .currency(execution.getCurrency())
                .referenceType(REFERENCE_TYPE)
                .referenceId(execution.getId().toString())
                .createdAt(LocalDateTime.now())
                .build());

        // Reverse: debit each recipient
        for (SplitPaymentLeg leg : execution.getLegs()) {
            entries.add(LedgerEntry.builder()
                    .id(UUID.randomUUID())
                    .accountId(leg.getRecipientAccountId())
                    .coaCode(COA_USER_WALLETS)
                    .entryType(EntryType.DEBIT)
                    .amount(leg.getAmount())
                    .currency(execution.getCurrency())
                    .referenceType(REFERENCE_TYPE)
                    .referenceId(execution.getId().toString())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        journalUseCase.createAndPostJournal(
                "Split payment reversal: " + reason,
                REFERENCE_TYPE,
                execution.getId().toString(),
                entries,
                "split-payment-service");
    }

    private String maskId(String id) {
        if (id == null || id.length() <= 4) return "****";
        return id.substring(0, 4) + "****";
    }
}
