package id.payu.integration.domain.repository;

import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for IntegrationMessage domain entity.
 * Follows hexagonal architecture - implementation is in adapter layer.
 */
public interface IntegrationMessageRepository {

    IntegrationMessage save(IntegrationMessage message);

    Optional<IntegrationMessage> findById(String messageId);

    List<IntegrationMessage> findByStatus(MessageStatus status);

    List<IntegrationMessage> findByType(MessageType type);

    List<IntegrationMessage> findByCorrelationId(String correlationId);

    /**
     * Find messages that need retry (failed and can be retried).
     */
    List<IntegrationMessage> findRetryableMessages();

    List<IntegrationMessage> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<IntegrationMessage> findByBusinessReference(String businessReference);

    long countByStatus(MessageStatus status);

    /**
     * Delete old messages (for data retention).
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
