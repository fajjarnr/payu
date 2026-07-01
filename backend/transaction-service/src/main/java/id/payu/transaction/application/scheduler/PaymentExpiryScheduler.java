package id.payu.transaction.application.scheduler;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.VirtualAccountPersistencePort;
import id.payu.outbox.service.OutboxService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import id.payu.transaction.domain.model.TransactionStatus;

/**
 * Scheduler to auto-cancel expired payments (VA, payment links, pending transactions).
 * Runs every 5 minutes to scan for pending payments that have passed their expiry time.
 *
 * Part of E-15 IMP-044: Payment Expiry & Auto-Cancel
 */
@Component
public class PaymentExpiryScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentExpiryScheduler.class);

    private final TransactionPersistencePort transactionPersistencePort;
    private final VirtualAccountPersistencePort virtualAccountPersistencePort;
    private final OutboxService outboxService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PaymentExpiryScheduler(TransactionPersistencePort transactionPersistencePort,
                                   VirtualAccountPersistencePort virtualAccountPersistencePort,
                                   OutboxService outboxService,
                                   ObjectMapper objectMapper) {
        this.transactionPersistencePort = transactionPersistencePort;
        this.virtualAccountPersistencePort = virtualAccountPersistencePort;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        // BUG-ARCH-006 FIX: Configure RestTemplate with timeouts instead of bare new RestTemplate()
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    private static final String WALLET_SERVICE_URL = "http://wallet-service/api/v1/wallets";
    private static final String PAYMENT_EXPIRED_TOPIC = "payu.transaction.payment-expired.v1";

    /**
     * Expire pending transactions that have passed their expiresAt timestamp.
     * Releases reserved balance and publishes payment.expired Kafka event.
     * ITER-53: ShedLock prevents double-execution on multi-replica deployment.
     */
    @SchedulerLock(name = "PaymentExpiryScheduler_expirePendingTransactions", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")
    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void expirePendingTransactions() {
        List<TransactionEntity> expired = transactionPersistencePort.findExpiredPendingTransactions(Instant.now());
        if (!expired.isEmpty()) {
            expired.forEach(tx -> {
                tx.setStatus(TransactionStatus.CANCELLED);
                tx.setFailureReason("Payment expired");
                tx.setUpdatedAt(Instant.now());

                // Release reserved balance if any
                releaseReservedBalance(tx);

                // Publish Kafka event
                publishPaymentExpiredEvent(tx);
            });
            transactionPersistencePort.saveAll(expired);
            log.info("Auto-cancelled {} expired transactions", expired.size());
        }
    }

    /**
     * Expire pending Virtual Accounts that have passed their TTL.
     * Publishes payment.expired Kafka event.
     * ITER-53: ShedLock prevents double-execution.
     */
    @SchedulerLock(name = "PaymentExpiryScheduler_expireVirtualAccounts", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")
    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void expireVirtualAccounts() {
        List<VirtualAccountEntity> expired = virtualAccountPersistencePort.findExpiredPendingVAs(Instant.now());
        if (!expired.isEmpty()) {
            expired.forEach(va -> {
                va.markExpired();
                publishVaExpiredEvent(va);
            });
            virtualAccountPersistencePort.saveAll(expired);
            log.info("Auto-expired {} virtual accounts", expired.size());
        }
    }

    /**
     * Release reserved balance for expired transaction.
     */
    private void releaseReservedBalance(TransactionEntity tx) {
        try {
            if (tx.getSourceAccountId() != null && tx.getAmount() != null) {
                // Call wallet-service to release reserved balance
                String url = WALLET_SERVICE_URL + "/" + tx.getSourceAccountId() + "/release";
                Map<String, Object> request = new HashMap<>();
                request.put("amount", tx.getAmount());
                request.put("transactionId", tx.getId().toString());
                request.put("reason", "Payment expired");

                restTemplate.postForEntity(url, request, Void.class);
                log.info("Released reserved balance for transaction {}: amount={}",
                    tx.getId(), tx.getAmount());
            }
        } catch (Exception e) {
            log.error("Failed to release reserved balance for transaction {}", tx.getId(), e);
        }
    }

    /**
     * Publish payment.expired Kafka event.
     */
    private void publishPaymentExpiredEvent(TransactionEntity tx) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "payment.expired");
            event.put("transactionId", tx.getId().toString());
            event.put("referenceId", tx.getReferenceId());
            event.put("amount", tx.getAmount());
            event.put("currency", tx.getCurrency());
            event.put("sourceAccountId", tx.getSourceAccountId());
            event.put("expiredAt", Instant.now().toString());
            event.put("reason", "Payment timeout");

            outboxService.createEvent(
                "Transaction",
                tx.getId().toString(),
                "PaymentExpired",
                event,
                null,
                PAYMENT_EXPIRED_TOPIC
            );

            log.info("Published payment.expired event for transaction {}", tx.getId());
        } catch (Exception e) {
            log.error("Failed to publish payment.expired event for transaction {}", tx.getId(), e);
        }
    }

    /**
     * Publish VA expired Kafka event.
     */
    private void publishVaExpiredEvent(VirtualAccountEntity va) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "va.expired");
            event.put("vaId", va.getId().toString());
            event.put("vaNumber", va.getVaNumber());
            event.put("amount", va.getAmount());
            event.put("currency", va.getCurrency());
            event.put("partnerId", va.getPartnerId());
            event.put("externalId", va.getExternalId());
            event.put("expiredAt", Instant.now().toString());

            outboxService.createEvent(
                "VirtualAccount",
                va.getId().toString(),
                "VirtualAccountExpired",
                event,
                null,
                PAYMENT_EXPIRED_TOPIC
            );

            log.info("Published va.expired event for VA {}", va.getVaNumber());
        } catch (Exception e) {
            log.error("Failed to publish va.expired event for VA {}", va.getVaNumber(), e);
        }
    }
}
