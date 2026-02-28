package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.*;
import id.payu.wallet.adapter.persistence.repository.SettlementBatchJpaRepository;
import id.payu.wallet.adapter.persistence.repository.RevenueSplitJpaRepository;
import id.payu.wallet.domain.model.*;
import id.payu.wallet.domain.port.out.SettlementPersistencePort;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for settlement operations.
 */
@Component
public class SettlementPersistenceAdapter implements SettlementPersistencePort {

    private final SettlementBatchJpaRepository settlementBatchRepository;
    private final RevenueSplitJpaRepository revenueSplitRepository;

    public SettlementPersistenceAdapter(
            SettlementBatchJpaRepository settlementBatchRepository,
            RevenueSplitJpaRepository revenueSplitRepository) {
        this.settlementBatchRepository = settlementBatchRepository;
        this.revenueSplitRepository = revenueSplitRepository;
    }

    @Override
    public SettlementBatch saveSettlementBatch(SettlementBatch batch) {
        SettlementBatchEntity entity = toSettlementBatchEntity(batch);
        SettlementBatchEntity saved = settlementBatchRepository.save(entity);
        return toSettlementBatchDomain(saved);
    }

    @Override
    public Optional<SettlementBatch> findSettlementBatchById(UUID id) {
        return settlementBatchRepository.findById(id)
                .map(this::toSettlementBatchDomain);
    }

    @Override
    public List<SettlementBatch> findSettlementBatchesByPartner(String partnerId, LocalDate from, LocalDate to) {
        return settlementBatchRepository.findByPartnerIdAndSettlementDateBetween(partnerId, from, to)
                .stream()
                .map(this::toSettlementBatchDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SettlementBatch> findSettlementBatchesByDate(LocalDate settlementDate) {
        return settlementBatchRepository.findBySettlementDate(settlementDate)
                .stream()
                .map(this::toSettlementBatchDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SettlementBatch> findPendingSettlementBatches() {
        return settlementBatchRepository.findPendingBatches()
                .stream()
                .map(this::toSettlementBatchDomain)
                .collect(Collectors.toList());
    }

    @Override
    public RevenueSplit saveRevenueSplit(RevenueSplit revenueSplit) {
        RevenueSplitEntity entity = toRevenueSplitEntity(revenueSplit);
        RevenueSplitEntity saved = revenueSplitRepository.save(entity);
        return toRevenueSplitDomain(saved);
    }

    @Override
    public Optional<RevenueSplit> findRevenueSplitById(UUID id) {
        return revenueSplitRepository.findById(id)
                .map(this::toRevenueSplitDomain);
    }

    @Override
    public List<RevenueSplit> findRevenueSplitsByPartner(String partnerId) {
        return revenueSplitRepository.findByPartnerId(partnerId)
                .stream()
                .map(this::toRevenueSplitDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RevenueSplit> findActiveRevenueSplitsByPartner(String partnerId) {
        return revenueSplitRepository.findActiveAndEffectiveByPartnerId(partnerId, LocalDateTime.now())
                .stream()
                .map(this::toRevenueSplitDomain)
                .collect(Collectors.toList());
    }

    // ---- Mappers ----

    private SettlementBatchEntity toSettlementBatchEntity(SettlementBatch domain) {
        SettlementBatchEntity entity = new SettlementBatchEntity();
        entity.setId(domain.getId());
        entity.setPartnerId(domain.getPartnerId());
        entity.setSettlementDate(domain.getSettlementDate());
        entity.setCurrency(domain.getCurrency());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setFeeAmount(domain.getFeeAmount());
        entity.setNetAmount(domain.getNetAmount());
        entity.setStatus(SettlementBatchEntity.SettlementStatus.valueOf(domain.getStatus().name()));
        entity.setReconciliationReport(domain.getReconciliationReport());
        entity.setFailureReason(domain.getFailureReason());
        entity.setProcessedBy(domain.getProcessedBy());
        entity.setProcessedAt(domain.getProcessedAt());
        entity.setTenantId(domain.getTenantId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        // Map entries
        if (domain.getEntries() != null) {
            List<SettlementEntryEntity> entryEntities = domain.getEntries().stream()
                    .map(e -> toSettlementEntryEntity(e, entity))
                    .collect(Collectors.toList());
            entity.setEntries(entryEntities);
        }

        // Map discrepancies
        if (domain.getDiscrepancies() != null) {
            List<DiscrepancyEntity> discrepancyEntities = domain.getDiscrepancies().stream()
                    .map(d -> toDiscrepancyEntity(d, entity))
                    .collect(Collectors.toList());
            entity.setDiscrepancies(discrepancyEntities);
        }

        return entity;
    }

    private SettlementBatch toSettlementBatchDomain(SettlementBatchEntity entity) {
        List<SettlementEntry> entries = entity.getEntries() != null
                ? entity.getEntries().stream().map(this::toSettlementEntryDomain).collect(Collectors.toList())
                : new java.util.ArrayList<>();

        List<Discrepancy> discrepancies = entity.getDiscrepancies() != null
                ? entity.getDiscrepancies().stream().map(this::toDiscrepancyDomain).collect(Collectors.toList())
                : new java.util.ArrayList<>();

        return SettlementBatch.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .settlementDate(entity.getSettlementDate())
                .currency(entity.getCurrency())
                .totalAmount(entity.getTotalAmount())
                .feeAmount(entity.getFeeAmount())
                .netAmount(entity.getNetAmount())
                .status(SettlementBatch.SettlementStatus.valueOf(entity.getStatus().name()))
                .entries(entries)
                .reconciliationReport(entity.getReconciliationReport())
                .discrepancies(discrepancies)
                .failureReason(entity.getFailureReason())
                .processedBy(entity.getProcessedBy())
                .processedAt(entity.getProcessedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .tenantId(entity.getTenantId())
                .build();
    }

    private SettlementEntryEntity toSettlementEntryEntity(SettlementEntry domain, SettlementBatchEntity batch) {
        SettlementEntryEntity entity = new SettlementEntryEntity();
        entity.setId(domain.getId());
        entity.setSettlementBatch(batch);
        entity.setTransactionId(domain.getTransactionId());
        entity.setReferenceType(domain.getReferenceType());
        entity.setReferenceId(domain.getReferenceId());
        entity.setAmount(domain.getAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setFee(domain.getFee());
        entity.setNetAmount(domain.getNetAmount());
        entity.setStatus(SettlementEntryEntity.EntryStatus.valueOf(domain.getStatus().name()));
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private SettlementEntry toSettlementEntryDomain(SettlementEntryEntity entity) {
        return SettlementEntry.builder()
                .id(entity.getId())
                .settlementBatchId(entity.getSettlementBatch() != null ? entity.getSettlementBatch().getId() : null)
                .transactionId(entity.getTransactionId())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .fee(entity.getFee())
                .netAmount(entity.getNetAmount())
                .status(SettlementEntry.EntryStatus.valueOf(entity.getStatus().name()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private DiscrepancyEntity toDiscrepancyEntity(Discrepancy domain, SettlementBatchEntity batch) {
        DiscrepancyEntity entity = new DiscrepancyEntity();
        entity.setId(domain.getId());
        entity.setSettlementBatch(batch);
        entity.setTransactionId(domain.getTransactionId());
        entity.setType(DiscrepancyEntity.DiscrepancyType.valueOf(domain.getType().name()));
        entity.setDescription(domain.getDescription());
        entity.setExpectedAmount(domain.getExpectedAmount());
        entity.setActualAmount(domain.getActualAmount());
        entity.setDifference(domain.getDifference());
        entity.setResolved(domain.isResolved());
        entity.setResolvedBy(domain.getResolvedBy());
        entity.setResolvedAt(domain.getResolvedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private Discrepancy toDiscrepancyDomain(DiscrepancyEntity entity) {
        return Discrepancy.builder()
                .id(entity.getId())
                .settlementBatchId(entity.getSettlementBatch() != null ? entity.getSettlementBatch().getId() : null)
                .transactionId(entity.getTransactionId())
                .type(Discrepancy.DiscrepancyType.valueOf(entity.getType().name()))
                .description(entity.getDescription())
                .expectedAmount(entity.getExpectedAmount())
                .actualAmount(entity.getActualAmount())
                .difference(entity.getDifference())
                .resolved(entity.isResolved())
                .resolvedBy(entity.getResolvedBy())
                .resolvedAt(entity.getResolvedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private RevenueSplitEntity toRevenueSplitEntity(RevenueSplit domain) {
        RevenueSplitEntity entity = new RevenueSplitEntity();
        entity.setId(domain.getId());
        entity.setPartnerId(domain.getPartnerId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setSplitType(RevenueSplitEntity.SplitType.valueOf(domain.getSplitType().name()));
        entity.setActive(domain.isActive());
        entity.setEffectiveFrom(domain.getEffectiveFrom());
        entity.setEffectiveUntil(domain.getEffectiveUntil());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setTenantId(domain.getTenantId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        if (domain.getStakeholders() != null) {
            List<StakeholderEntity> stakeholderEntities = domain.getStakeholders().stream()
                    .map(s -> toStakeholderEntity(s, entity))
                    .collect(Collectors.toList());
            entity.setStakeholders(stakeholderEntities);
        }

        return entity;
    }

    private RevenueSplit toRevenueSplitDomain(RevenueSplitEntity entity) {
        List<Stakeholder> stakeholders = entity.getStakeholders() != null
                ? entity.getStakeholders().stream().map(this::toStakeholderDomain).collect(Collectors.toList())
                : new java.util.ArrayList<>();

        return RevenueSplit.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .name(entity.getName())
                .description(entity.getDescription())
                .splitType(RevenueSplit.SplitType.valueOf(entity.getSplitType().name()))
                .stakeholders(stakeholders)
                .active(entity.isActive())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveUntil(entity.getEffectiveUntil())
                .createdBy(entity.getCreatedBy())
                .tenantId(entity.getTenantId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private StakeholderEntity toStakeholderEntity(Stakeholder domain, RevenueSplitEntity split) {
        StakeholderEntity entity = new StakeholderEntity();
        entity.setId(domain.getId());
        entity.setRevenueSplit(split);
        entity.setAccountId(domain.getAccountId());
        entity.setName(domain.getName());
        entity.setPercentage(domain.getPercentage());
        entity.setFixedAmount(domain.getFixedAmount());
        entity.setPriority(domain.getPriority());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private Stakeholder toStakeholderDomain(StakeholderEntity entity) {
        return Stakeholder.builder()
                .id(entity.getId())
                .revenueSplitId(entity.getRevenueSplit() != null ? entity.getRevenueSplit().getId() : null)
                .accountId(entity.getAccountId())
                .name(entity.getName())
                .percentage(entity.getPercentage())
                .fixedAmount(entity.getFixedAmount())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
