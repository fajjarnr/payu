package id.payu.transaction.application.scheduler;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.TransferStatusPort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ReconciliationScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    private final TransactionPersistencePort transactionPersistencePort;
    private final TransferStatusPort transferStatusPort;
    private final WalletServicePort walletServicePort;

    public ReconciliationScheduler(TransactionPersistencePort transactionPersistencePort,
                                   TransferStatusPort transferStatusPort,
                                   WalletServicePort walletServicePort) {
        this.transactionPersistencePort = transactionPersistencePort;
        this.transferStatusPort = transferStatusPort;
        this.walletServicePort = walletServicePort;
    }

    /**
     * PADG 14/2025 + ADR-0060: intra-day reconciliation every 5m, T+1 full.
     * Auto-heal PENDING >5m via GET /snap/v1.0/transfer/status (BRIAPI 00/01/03/06).
     */
    @SchedulerLock(name = "biFastReconciliation", lockAtMostFor = "9m", lockAtLeastFor = "30s")
    @Scheduled(fixedDelay = 300000)
    public void reconcilePendingTransfers() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<TransactionEntity> candidates = transactionPersistencePort.findPendingOlderThan(cutoff);
        if (candidates.isEmpty()) {
            log.debug("Reconciliation: no PENDING >5m candidates");
            return;
        }
        log.info("Reconciliation: found {} PENDING >5m candidates", candidates.size());
        for (TransactionEntity tx : candidates) {
            try {
                String latest = transferStatusPort.getLatestTransactionStatus(tx.getReferenceNumber());
                log.info("Reconciliation polling referenceNo={} railStatus={}", tx.getReferenceNumber(), latest);
                switch (latest) {
                    case "00", "SUCCESS", "COMPLETED", "SETTLED" -> healCompleted(tx);
                    case "03", "06", "FAILED", "REJECTED" -> healFailed(tx, "Reconciled as FAILED from rail " + latest);
                    case "01", "PENDING", "PROCESSING" -> { /* still pending */ }
                    default -> log.warn("Reconciliation unknown status {} for {}", latest, tx.getReferenceNumber());
                }
            } catch (Exception e) {
                log.warn("Reconciliation failed for {}: {}", tx.getReferenceNumber(), e.getMessage());
            }
        }
    }

    private void healCompleted(TransactionEntity tx) {
        if (tx.getStatus() == TransactionStatus.COMPLETED) return;
        if (tx.getType() == TransactionType.INTERNAL_TRANSFER) {
            tx.setStatus(TransactionStatus.COMPLETED);
            tx.setCompletedAt(Instant.now());
            transactionPersistencePort.save(tx);
            return;
        }
        if (tx.getReservationId() != null) {
            try {
                walletServicePort.commitBalance(tx.getSenderAccountId(), tx.getId().toString(), tx.getReservationId(), tx.getAmount().getAmount());
            } catch (Exception e) {
                log.warn("Reconciliation commit failed for {}: {}", tx.getReferenceNumber(), e.getMessage());
            }
        }
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setCompletedAt(Instant.now());
        transactionPersistencePort.save(tx);
        log.info("Reconciliation auto-heal COMPLETED {}", tx.getReferenceNumber());
    }

    private void healFailed(TransactionEntity tx, String reason) {
        if (tx.getStatus() == TransactionStatus.FAILED) return;
        if (tx.getReservationId() != null) {
            try {
                walletServicePort.releaseBalance(tx.getSenderAccountId(), tx.getId().toString(), tx.getReservationId(), tx.getAmount().getAmount());
            } catch (Exception e) {
                log.warn("Reconciliation release failed for {}: {}", tx.getReferenceNumber(), e.getMessage());
            }
        }
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureReason(reason);
        transactionPersistencePort.save(tx);
        log.info("Reconciliation auto-heal FAILED {}", tx.getReferenceNumber());
    }
}
