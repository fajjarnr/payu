package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.*;
import id.payu.promotion.domain.port.out.CashbackRuleRepositoryPort;
import id.payu.promotion.domain.port.out.CashbackRecordRepositoryPort;
import id.payu.promotion.domain.port.out.WalletServicePort;
import id.payu.promotion.domain.port.out.NotificationPort;
import id.payu.promotion.interfaces.dto.TransactionCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import id.payu.promotion.domain.model.CashbackStatus;

/**
 * Application service for processing cashback on transaction completion.
 * Listens to transaction events and applies matching cashback rules.
 */
@Service
public class CashbackProcessorService implements id.payu.promotion.application.port.in.ProcessCashbackUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CashbackProcessorService.class);

    private final CashbackRuleRepositoryPort cashbackRuleRepository;
    private final CashbackRecordRepositoryPort cashbackRecordRepository;
    private final WalletServicePort walletServicePort;
    private final NotificationPort notificationPort;

    public CashbackProcessorService(
            CashbackRuleRepositoryPort cashbackRuleRepository,
            CashbackRecordRepositoryPort cashbackRecordRepository,
            WalletServicePort walletServicePort,
            NotificationPort notificationPort) {
        this.cashbackRuleRepository = cashbackRuleRepository;
        this.cashbackRecordRepository = cashbackRecordRepository;
        this.walletServicePort = walletServicePort;
        this.notificationPort = notificationPort;
    }

    /**
     * Processes cashback for a completed transaction.
     * Evaluates all active rules and credits wallet for matching rules.
     *
     * @param event the transaction completed event
     * @return the result of cashback processing
     */
    @Transactional
    public CashbackResult process(TransactionCompletedEvent event) {
        LOG.info("Processing cashback for transaction: {}, account: {}",
                event.transactionId(), event.accountId());

        // Check if already processed
        if (cashbackRecordRepository.hasProcessedTransaction(event.transactionId())) {
            LOG.info("Transaction already processed for cashback: {}", event.transactionId());
            return CashbackResult.empty();
        }

        // Build transaction domain object
        Transaction transaction = Transaction.builder()
                .transactionId(event.transactionId())
                .accountId(event.accountId())
                .amount(event.amount())
                .merchantCode(event.merchantCode())
                .categoryCode(event.categoryCode())
                .timestamp(event.timestamp())
                .build();

        // Find active rules
        List<CashbackRule> activeRules = cashbackRuleRepository.findActiveRules();
        LOG.debug("Found {} active cashback rules", activeRules.size());

        int processedCount = 0;
        int matchedCount = 0;
        BigDecimal totalCashback = BigDecimal.ZERO;
        List<String> processedRuleIds = new ArrayList<>();

        // Evaluate each rule
        for (CashbackRule rule : activeRules) {
            if (rule.matches(transaction)) {
                matchedCount++;
                BigDecimal cashbackAmount = rule.calculateCashback(transaction);

                if (cashbackAmount.compareTo(BigDecimal.ZERO) > 0) {
                    boolean credited = processCashbackForRule(event, rule, cashbackAmount);

                    if (credited) {
                        processedCount++;
                        totalCashback = totalCashback.add(cashbackAmount);
                        processedRuleIds.add(rule.getRuleId());
                    }
                }
            }
        }

        LOG.info("CashbackEntity processing complete for transaction: {}, processed: {}, total: {}",
                event.transactionId(), processedCount, totalCashback);

        return CashbackResult.builder()
                .success(processedCount > 0 || matchedCount == 0)
                .processedCount(processedCount)
                .totalCashbackAmount(totalCashback)
                .processedRuleIds(processedRuleIds)
                .build();
    }

    /**
     * Processes cashback for a specific rule.
     * <p>
     * PROMO-DOUBLE-001: the cashback record is persisted BEFORE the wallet credit
     * so a failure mid-flow leaves a durable intent (PENDING) that the retry path
     * can resume instead of silently losing the record. The wallet credit is
     * idempotent by referenceId (WalletService.validateCreditReplay), so re-processing
     * the same event can never double-credit. Exceptions are rethrown so the Kafka
     * consumer retries and forwards to DLQ rather than acking a lost record.
     *
     * @param event the transaction event
     * @param rule the cashback rule
     * @param amount the cashback amount
     * @return true if cashback was successfully credited
     */
    private boolean processCashbackForRule(TransactionCompletedEvent event, CashbackRule rule, BigDecimal amount) {
        String referenceId = event.transactionId() + "-" + rule.getRuleId();

        CashbackRecord record = cashbackRecordRepository.findByTransactionIdAndRuleId(
                        event.transactionId(), rule.getRuleId())
                .orElseGet(CashbackRecord::new);
        record.setTransactionId(event.transactionId());
        record.setAccountId(event.accountId());
        record.setRuleId(rule.getRuleId());
        record.setCashbackAmount(amount);
        record.setWalletReferenceId(referenceId);
        record.setStatus(CashbackStatus.PENDING);
        record = cashbackRecordRepository.save(record);

        boolean credited = walletServicePort.creditWallet(
                event.accountId(),
                amount,
                referenceId,
                "CashbackEntity for transaction " + event.transactionId() + " via rule " + rule.getRuleId()
        );

        if (!credited) {
            record.setStatus(CashbackStatus.FAILED);
            cashbackRecordRepository.save(record);
            LOG.error("Failed to credit wallet for transaction: {}, rule: {}",
                    event.transactionId(), rule.getRuleId());
            return false;
        }

        record.setStatus(CashbackStatus.CREDITED);
        cashbackRecordRepository.save(record);

        // Send notification
        CashbackNotification notification = new CashbackNotification(
                event.accountId(),
                event.transactionId(),
                amount,
                "You received " + amount + " cashback for your transaction!"
        );
        notificationPort.sendCashbackNotification(notification);

        LOG.info("CashbackEntity credited: transaction={}, rule={}, amount={}",
                event.transactionId(), rule.getRuleId(), amount);

        return true;
    }
}
