package id.payu.transaction.application.scheduler;

import id.payu.transaction.adapter.persistence.repository.TransactionJpaRepository;
import id.payu.transaction.adapter.persistence.repository.VirtualAccountRepository;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.model.VirtualAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler to auto-cancel expired payments (VA, payment links, pending transactions).
 * Runs every 5 minutes to scan for pending payments that have passed their expiry time.
 *
 * Part of E-15 IMP-044: Payment Expiry & Auto-Cancel
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private final TransactionJpaRepository transactionRepository;
    private final VirtualAccountRepository virtualAccountRepository;

    /**
     * Expire pending transactions that have passed their expiresAt timestamp.
     * Publishes payment.expired Kafka event for webhook notification.
     */
    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void expirePendingTransactions() {
        List<Transaction> expired = transactionRepository.findExpiredPendingTransactions(Instant.now());
        if (!expired.isEmpty()) {
            expired.forEach(tx -> {
                tx.setStatus(Transaction.TransactionStatus.CANCELLED);
                tx.setFailureReason("Payment expired");
                tx.setUpdatedAt(Instant.now());
            });
            transactionRepository.saveAll(expired);
            log.info("Auto-cancelled {} expired transactions", expired.size());
        }
    }

    /**
     * Expire pending Virtual Accounts that have passed their TTL.
     */
    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void expireVirtualAccounts() {
        List<VirtualAccount> expired = virtualAccountRepository.findExpiredPendingVAs(Instant.now());
        if (!expired.isEmpty()) {
            expired.forEach(VirtualAccount::markExpired);
            virtualAccountRepository.saveAll(expired);
            log.info("Auto-expired {} virtual accounts", expired.size());
        }
    }
}
