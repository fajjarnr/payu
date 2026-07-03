package id.payu.integration.application.service;

import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageDirection;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;
import id.payu.integration.domain.repository.IntegrationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service for processing integration messages.
 * Handles message lifecycle and business rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessingService {

    private final IntegrationMessageRepository messageRepository;

    /**
     * Save an already-constructed IntegrationMessage.
     * Used by Camel routes that build the full message in-processor.
     */
    @Transactional
    public IntegrationMessage createMessage(IntegrationMessage message) {
        message.setCreatedAt(LocalDateTime.now());
        if (message.getStatus() == null) {
            message.setStatus(MessageStatus.RECEIVED);
        }
        return messageRepository.save(message);
    }

    /**
     * Create a new integration message from individual fields.
     */
    @Transactional
    public IntegrationMessage createMessage(MessageType type,
                                            MessageDirection direction,
                                            String sourceSystem,
                                            String targetSystem,
                                            String rawPayload,
                                            String correlationId,
                                            String businessReference) {
        IntegrationMessage message = IntegrationMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .type(type)
                .direction(direction)
                .sourceSystem(sourceSystem)
                .targetSystem(targetSystem)
                .rawPayload(rawPayload)
                .correlationId(correlationId)
                .businessReference(businessReference)
                .status(MessageStatus.RECEIVED)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    /**
     * Validate a message.
     */
    @Transactional
    public void validateMessage(String messageId) {
        IntegrationMessage message = findMessageOrThrow(messageId);
        message.markValidating();
        messageRepository.save(message);
        log.debug("Message {} marked as validating", messageId);
    }

    /**
     * Transform a message.
     */
    @Transactional
    public void transformMessage(String messageId, String transformedPayload) {
        IntegrationMessage message = findMessageOrThrow(messageId);
        message.markTransforming();
        messageRepository.save(message);

        message.markTransformed(transformedPayload);
        messageRepository.save(message);
        log.debug("Message {} transformed successfully", messageId);
    }

    /**
     * Mark message as sent.
     */
    @Transactional
    public void markSent(String messageId) {
        IntegrationMessage message = findMessageOrThrow(messageId);
        message.markSent();
        messageRepository.save(message);
        log.info("Message {} marked as sent", messageId);
    }

    /**
     * Mark message as failed.
     */
    @Transactional
    public void markFailed(String messageId, String errorMessage) {
        IntegrationMessage message = findMessageOrThrow(messageId);
        message.markFailed(errorMessage);
        messageRepository.save(message);
        log.error("Message {} failed: {}", messageId, errorMessage);
    }

    /**
     * Retry a failed message.
     */
    @Transactional
    public boolean retryMessage(String messageId) {
        IntegrationMessage message = findMessageOrThrow(messageId);

        if (!message.canRetry()) {
            log.warn("Message {} has exceeded max retries", messageId);
            return false;
        }

        message.markRetrying();
        message.setStatus(MessageStatus.RECEIVED);
        messageRepository.save(message);
        log.info("Message {} queued for retry (attempt {})", messageId, message.getRetryCount());
        return true;
    }

    /**
     * Get message by ID.
     */
    @Transactional(readOnly = true)
    public Optional<IntegrationMessage> getMessage(String messageId) {
        return messageRepository.findById(messageId);
    }

    /**
     * Get messages by status.
     */
    @Transactional(readOnly = true)
    public List<IntegrationMessage> getMessagesByStatus(MessageStatus status) {
        return messageRepository.findByStatus(status);
    }

    /**
     * Get retryable messages.
     */
    @Transactional(readOnly = true)
    public List<IntegrationMessage> getRetryableMessages() {
        return messageRepository.findRetryableMessages();
    }

    /**
     * Get message status.
     */
    @Transactional(readOnly = true)
    public MessageStatus getMessageStatus(String messageId) {
        return findMessageOrThrow(messageId).getStatus();
    }

    private IntegrationMessage findMessageOrThrow(String messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));
    }

    /**
     * Exception thrown when message is not found.
     */
    public static class MessageNotFoundException extends RuntimeException {
        public MessageNotFoundException(String message) {
            super(message);
        }
    }
}
