package id.payu.transaction.application.service;

import id.payu.outbox.service.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/**
 * TXN-HARDEN-003: publish outbox outside the business TX.
 * Uses afterCommit hook + REQUIRES_NEW so the outbox row is NOT in same DB TX as business mutation
 * (never call rail/outbox inside TX per rockthejvm).
 */
@Service
public class DeferredOutboxService {
    private final OutboxService outboxService;
    private final TransactionTemplate requiresNewTemplate;

    public DeferredOutboxService(OutboxService outboxService, PlatformTransactionManager txManager) {
        this.outboxService = outboxService;
        this.requiresNewTemplate = new TransactionTemplate(txManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void publishAfterCommit(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload, String destinationTopic) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    requiresNewTemplate.executeWithoutResult(tx ->
                            outboxService.createEvent(aggregateType, aggregateId, eventType, payload, null, destinationTopic));
                }
            });
        } else {
            requiresNewTemplate.executeWithoutResult(tx ->
                    outboxService.createEvent(aggregateType, aggregateId, eventType, payload, null, destinationTopic));
        }
    }

    public void publishAfterCommitWithHeaders(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload, Map<String, Object> headers, String destinationTopic) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    requiresNewTemplate.executeWithoutResult(tx ->
                            outboxService.createEvent(aggregateType, aggregateId, eventType, payload, headers, destinationTopic));
                }
            });
        } else {
            requiresNewTemplate.executeWithoutResult(tx ->
                    outboxService.createEvent(aggregateType, aggregateId, eventType, payload, headers, destinationTopic));
        }
    }
}
