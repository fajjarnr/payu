package id.payu.integration.adapter.persistence;

import id.payu.integration.adapter.persistence.entity.IntegrationMessageEntity;
import id.payu.integration.adapter.persistence.repository.IntegrationMessageJpaRepository;
import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;
import id.payu.integration.domain.repository.IntegrationMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of IntegrationMessageRepository.
 * Adapts domain repository interface to JPA repository.
 */
@Component
@RequiredArgsConstructor
public class IntegrationMessageRepositoryImpl implements IntegrationMessageRepository {

    private final IntegrationMessageJpaRepository jpaRepository;

    @Override
    @Transactional
    public IntegrationMessage save(IntegrationMessage message) {
        IntegrationMessageEntity entity = toEntity(message);
        IntegrationMessageEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IntegrationMessage> findById(String messageId) {
        return jpaRepository.findById(messageId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationMessage> findByStatus(MessageStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationMessage> findByType(MessageType type) {
        return jpaRepository.findByType(type).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationMessage> findByCorrelationId(String correlationId) {
        return jpaRepository.findByCorrelationId(correlationId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationMessage> findRetryableMessages() {
        return jpaRepository.findRetryableMessages().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationMessage> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCreatedAtBetween(start, end).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IntegrationMessage> findByBusinessReference(String businessReference) {
        return jpaRepository.findByBusinessReference(businessReference).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(MessageStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    @Transactional
    public void deleteByCreatedAtBefore(LocalDateTime cutoff) {
        jpaRepository.deleteByCreatedAtBefore(cutoff);
    }

    private IntegrationMessageEntity toEntity(IntegrationMessage message) {
        return IntegrationMessageEntity.builder()
                .messageId(message.getMessageId())
                .type(message.getType())
                .direction(message.getDirection())
                .sourceSystem(message.getSourceSystem())
                .targetSystem(message.getTargetSystem())
                .correlationId(message.getCorrelationId())
                .businessReference(message.getBusinessReference())
                .rawPayload(message.getRawPayload())
                .transformedPayload(message.getTransformedPayload())
                .status(message.getStatus())
                .errorMessage(message.getErrorMessage())
                .retryCount(message.getRetryCount())
                .maxRetries(message.getMaxRetries())
                .createdAt(message.getCreatedAt())
                .processedAt(message.getProcessedAt())
                .lastRetryAt(message.getLastRetryAt())
                .version(message.getVersion())
                .build();
    }

    private IntegrationMessage toDomain(IntegrationMessageEntity entity) {
        return IntegrationMessage.builder()
                .messageId(entity.getMessageId())
                .type(entity.getType())
                .direction(entity.getDirection())
                .sourceSystem(entity.getSourceSystem())
                .targetSystem(entity.getTargetSystem())
                .correlationId(entity.getCorrelationId())
                .businessReference(entity.getBusinessReference())
                .rawPayload(entity.getRawPayload())
                .transformedPayload(entity.getTransformedPayload())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .retryCount(entity.getRetryCount())
                .maxRetries(entity.getMaxRetries())
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getProcessedAt())
                .lastRetryAt(entity.getLastRetryAt())
                .version(entity.getVersion())
                .build();
    }
}
