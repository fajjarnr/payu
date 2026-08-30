package id.payu.dispute.adapter.persistence;

import id.payu.dispute.adapter.persistence.entity.RefundEntity;
import id.payu.dispute.adapter.persistence.repository.RefundJpaRepository;
import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.RefundStatus;
import id.payu.dispute.domain.port.out.RefundPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for Refund entities.
 *
 * <p>Implements the RefundPersistencePort to provide JPA-based persistence.
 * Handles mapping between domain model and entity.</p>
 */
@Component
@RequiredArgsConstructor
public class RefundPersistenceAdapter implements RefundPersistencePort {

    private final RefundJpaRepository refundJpaRepository;

    @Override
    public Refund save(Refund refund) {
        RefundEntity entity = refund.getId() == null
                ? toEntity(refund)
                : refundJpaRepository.findById(refund.getId())
                        .map(existing -> updateEntity(existing, refund))
                        .orElseGet(() -> toEntity(refund));
        RefundEntity saved = refundJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Refund> findById(UUID id) {
        return refundJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Refund> findByTransactionId(UUID transactionId) {
        return refundJpaRepository.findByTransactionId(transactionId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Refund> findByStatus(RefundStatus status) {
        return refundJpaRepository.findByStatus(status)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void lockTransaction(UUID transactionId) {
        refundJpaRepository.lockTransaction(transactionId.toString());
    }

    @Override
    public List<Refund> findAll() {
        return refundJpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        refundJpaRepository.deleteById(id);
    }

    private RefundEntity toEntity(Refund refund) {
        return RefundEntity.builder()
                .id(refund.getId())
                .transactionId(refund.getTransactionId())
                .idempotencyKey(refund.getIdempotencyKey())
                .amount(refund.getAmount())
                .currency(refund.getCurrency())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .failureReason(refund.getFailureReason())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .completedAt(refund.getCompletedAt())
                .failedAt(refund.getFailedAt())
                .cancelledAt(refund.getCancelledAt())
                .build();
    }

    private RefundEntity updateEntity(RefundEntity entity, Refund refund) {
        entity.setTransactionId(refund.getTransactionId());
        entity.setIdempotencyKey(refund.getIdempotencyKey());
        entity.setAmount(refund.getAmount());
        entity.setCurrency(refund.getCurrency());
        entity.setReason(refund.getReason());
        entity.setStatus(refund.getStatus());
        entity.setFailureReason(refund.getFailureReason());
        entity.setCreatedAt(refund.getCreatedAt());
        entity.setProcessedAt(refund.getProcessedAt());
        entity.setCompletedAt(refund.getCompletedAt());
        entity.setFailedAt(refund.getFailedAt());
        entity.setCancelledAt(refund.getCancelledAt());
        return entity;
    }

    private Refund toDomain(RefundEntity entity) {
        return Refund.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .idempotencyKey(entity.getIdempotencyKey())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getProcessedAt())
                .completedAt(entity.getCompletedAt())
                .failedAt(entity.getFailedAt())
                .cancelledAt(entity.getCancelledAt())
                .build();
    }
}
