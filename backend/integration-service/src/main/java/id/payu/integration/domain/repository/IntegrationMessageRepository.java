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

    /**
     * Save an integration message.
     */
    IntegrationMessage save(IntegrationMessage message);

    /**
     * Find message by ID.
     */
    Optional<IntegrationMessage> findById(String messageId);

    /**
     * Find messages by status.
     */
    List<IntegrationMessage> findByStatus(MessageStatus status);

    /**
     * Find messages by type.
     */
    List<IntegrationMessage> findByType(MessageType type);

    /**
     * Find messages by correlation ID.
     */
    List<IntegrationMessage> findByCorrelationId(String correlationId);

    /**
     * Find messages that need retry (failed and can be retried).
     */
    List<IntegrationMessage> findRetryableMessages();

    /**
     * Find messages created within a time range.
     */
    List<IntegrationMessage> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find messages by business reference.
     */
    Optional<IntegrationMessage> findByBusinessReference(String businessReference);

    /**
     * Count messages by status.
     */
    long countByStatus(MessageStatus status);

    /**
     * Delete old messages (for data retention).
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
