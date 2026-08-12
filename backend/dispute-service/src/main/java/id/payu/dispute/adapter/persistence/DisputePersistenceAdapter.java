package id.payu.dispute.adapter.persistence;

import id.payu.dispute.adapter.persistence.entity.DisputeEntity;
import id.payu.dispute.adapter.persistence.entity.DisputeEvidenceEntity;
import id.payu.dispute.adapter.persistence.repository.DisputeJpaRepository;
import id.payu.dispute.domain.model.Dispute;
import id.payu.dispute.domain.model.DisputeEvidence;
import id.payu.dispute.domain.model.DisputeStatus;
import id.payu.dispute.domain.port.out.DisputePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for Dispute entities.
 *
 * <p>Implements the DisputePersistencePort to provide JPA-based persistence.
 * Handles mapping between domain model and entity.</p>
 */
@Component
@RequiredArgsConstructor
public class DisputePersistenceAdapter implements DisputePersistencePort {

    private final DisputeJpaRepository disputeJpaRepository;

    @Override
    public Dispute save(Dispute dispute) {
        // DISPUTE-002: with @Version, JpaRepository.save() on a NEW instance that
        // already has an id (re-save of a loaded dispute) is treated as persist ->
        // EntityExistsException. Update the managed entity in place instead.
        DisputeEntity entity = dispute.getId() == null
                ? toEntity(dispute)
                : disputeJpaRepository.findById(dispute.getId())
                        .map(existing -> updateEntity(existing, dispute))
                        .orElseGet(() -> toEntity(dispute));
        DisputeEntity saved = disputeJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Dispute> findById(UUID id) {
        return disputeJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Dispute> findByIdAndCustomerId(UUID id, UUID customerId) {
        return disputeJpaRepository.findByIdAndCustomerId(id, customerId).map(this::toDomain);
    }

    @Override
    public List<Dispute> findByTransactionId(UUID transactionId) {
        return disputeJpaRepository.findByTransactionId(transactionId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dispute> findByTransactionIdAndCustomerId(UUID transactionId, UUID customerId) {
        return disputeJpaRepository.findByTransactionIdAndCustomerId(transactionId, customerId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dispute> findByCustomerId(UUID customerId) {
        return disputeJpaRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dispute> findByMerchantId(UUID merchantId) {
        return disputeJpaRepository.findByMerchantId(merchantId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dispute> findByStatus(DisputeStatus status) {
        return disputeJpaRepository.findByStatus(status)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dispute> findAll() {
        return disputeJpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        disputeJpaRepository.deleteById(id);
    }

    // DISPUTE-002: copy state onto the managed entity. The evidence collection
    // is kept as the SAME collection instance (orphan-removal breaks when the
    // reference is replaced); orphan deletion + reinsert keeps rows consistent.
    private DisputeEntity updateEntity(DisputeEntity entity, Dispute dispute) {
        entity.setTransactionId(dispute.getTransactionId());
        entity.setCustomerId(dispute.getCustomerId());
        entity.setMerchantId(dispute.getMerchantId());
        entity.setDisputedAmount(dispute.getDisputedAmount());
        entity.setCurrency(dispute.getCurrency());
        entity.setReason(dispute.getReason());
        entity.setStatus(dispute.getStatus());
        entity.setInvestigationId(dispute.getInvestigationId());
        entity.setResolutionType(dispute.getResolutionType());
        entity.setResolution(dispute.getResolution());
        entity.setRejectionReason(dispute.getRejectionReason());
        entity.setEscalationReason(dispute.getEscalationReason());
        entity.setOpenedAt(dispute.getOpenedAt());
        entity.setInvestigationStartedAt(dispute.getInvestigationStartedAt());
        entity.setResolvedAt(dispute.getResolvedAt());
        entity.setRejectedAt(dispute.getRejectedAt());
        entity.setEscalatedAt(dispute.getEscalatedAt());
        entity.getEvidenceList().clear();
        dispute.getEvidenceList().forEach(evidence ->
                entity.getEvidenceList().add(toEvidenceEntity(evidence, entity)));
        return entity;
    }

    private DisputeEntity toEntity(Dispute dispute) {
        DisputeEntity entity = DisputeEntity.builder()
                .id(dispute.getId())
                .transactionId(dispute.getTransactionId())
                .customerId(dispute.getCustomerId())
                .merchantId(dispute.getMerchantId())
                .disputedAmount(dispute.getDisputedAmount())
                .currency(dispute.getCurrency())
                .reason(dispute.getReason())
                .status(dispute.getStatus())
                .investigationId(dispute.getInvestigationId())
                .resolutionType(dispute.getResolutionType())
                .resolution(dispute.getResolution())
                .rejectionReason(dispute.getRejectionReason())
                .escalationReason(dispute.getEscalationReason())
                .openedAt(dispute.getOpenedAt())
                .investigationStartedAt(dispute.getInvestigationStartedAt())
                .resolvedAt(dispute.getResolvedAt())
                .rejectedAt(dispute.getRejectedAt())
                .escalatedAt(dispute.getEscalatedAt())
                .evidenceList(new ArrayList<>())
                .build();
        dispute.getEvidenceList().forEach(evidence ->
                entity.getEvidenceList().add(toEvidenceEntity(evidence, entity)));
        return entity;
    }

    private Dispute toDomain(DisputeEntity entity) {
        List<DisputeEvidence> evidenceList = entity.getEvidenceList().stream()
                .map(this::toEvidenceDomain)
                .collect(Collectors.toList());

        return Dispute.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .customerId(entity.getCustomerId())
                .merchantId(entity.getMerchantId())
                .disputedAmount(entity.getDisputedAmount())
                .currency(entity.getCurrency())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .investigationId(entity.getInvestigationId())
                .resolutionType(entity.getResolutionType())
                .resolution(entity.getResolution())
                .rejectionReason(entity.getRejectionReason())
                .escalationReason(entity.getEscalationReason())
                .openedAt(entity.getOpenedAt())
                .investigationStartedAt(entity.getInvestigationStartedAt())
                .resolvedAt(entity.getResolvedAt())
                .rejectedAt(entity.getRejectedAt())
                .escalatedAt(entity.getEscalatedAt())
                .evidenceList(evidenceList)
                .build();
    }

    private DisputeEvidenceEntity toEvidenceEntity(DisputeEvidence evidence, DisputeEntity parent) {
        return DisputeEvidenceEntity.builder()
                .id(evidence.getId())
                .dispute(parent)
                .fileName(evidence.getFileName())
                .fileUrl(evidence.getFileUrl())
                .uploadedBy(evidence.getUploadedBy())
                .uploadedAt(evidence.getUploadedAt())
                .build();
    }

    private DisputeEvidence toEvidenceDomain(DisputeEvidenceEntity entity) {
        return DisputeEvidence.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .fileUrl(entity.getFileUrl())
                .uploadedBy(entity.getUploadedBy())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }
}
