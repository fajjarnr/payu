package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.*;
import id.payu.promotion.domain.port.out.CashbackRuleRepositoryPort;
import id.payu.promotion.domain.port.out.CashbackRecordRepositoryPort;
import id.payu.promotion.domain.port.out.WalletServicePort;
import id.payu.promotion.domain.port.out.NotificationPort;
import id.payu.promotion.dto.TransactionCompletedEvent;
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
     *
     * @param event the transaction event
     * @param rule the cashback rule
     * @param amount the cashback amount
     * @return true if cashback was successfully credited
     */
    private boolean processCashbackForRule(TransactionCompletedEvent event, CashbackRule rule, BigDecimal amount) {
        String referenceId = event.transactionId() + "-" + rule.getRuleId();

        try {
            // Credit wallet
            boolean credited = walletServicePort.creditWallet(
                    event.accountId(),
                    amount,
                    referenceId,
                    "CashbackEntity for transaction " + event.transactionId() + " via rule " + rule.getRuleId()
            );

            if (!credited) {
                LOG.error("Failed to credit wallet for transaction: {}, rule: {}",
                        event.transactionId(), rule.getRuleId());
                return false;
            }

            // Record cashback
            CashbackRecord record = new CashbackRecord();
            record.setId(UUID.randomUUID().toString());
            record.setTransactionId(event.transactionId());
            record.setAccountId(event.accountId());
            record.setRuleId(rule.getRuleId());
            record.setCashbackAmount(amount);
            record.setStatus(CashbackStatus.CREDITED);
            record.setWalletReferenceId(referenceId);

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

        } catch (Exception e) {
            LOG.error("Exception processing cashback for transaction: {}, rule: {}",
                    event.transactionId(), rule.getRuleId(), e);
            return false;
        }
    }
}
