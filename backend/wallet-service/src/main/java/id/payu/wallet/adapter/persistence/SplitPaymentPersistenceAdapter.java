package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.*;
import id.payu.wallet.adapter.persistence.repository.SplitPaymentExecutionRepository;
import id.payu.wallet.adapter.persistence.repository.SplitPaymentRuleRepository;
import id.payu.wallet.domain.model.SplitPaymentExecution;
import id.payu.wallet.domain.model.SplitPaymentLeg;
import id.payu.wallet.domain.model.SplitPaymentRule;
import id.payu.wallet.domain.model.SplitRecipient;
import id.payu.wallet.domain.model.SplitExecutionStatus;
import id.payu.wallet.domain.port.out.SplitPaymentPersistencePort;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SplitPaymentPersistenceAdapter implements SplitPaymentPersistencePort {

    private final SplitPaymentRuleRepository ruleRepository;
    private final SplitPaymentExecutionRepository executionRepository;

    public SplitPaymentPersistenceAdapter(SplitPaymentRuleRepository ruleRepository,
                                           SplitPaymentExecutionRepository executionRepository) {
        this.ruleRepository = ruleRepository;
        this.executionRepository = executionRepository;
    }

    // --- Rules ---

    @Override
    public SplitPaymentRule saveRule(SplitPaymentRule rule) {
        SplitPaymentRuleEntity entity = toRuleEntity(rule);
        SplitPaymentRuleEntity saved = ruleRepository.save(entity);
        return toRuleDomain(saved);
    }

    @Override
    public Optional<SplitPaymentRule> findRuleById(UUID ruleId) {
        return ruleRepository.findById(ruleId).map(this::toRuleDomain);
    }

    @Override
    public List<SplitPaymentRule> findRulesByPartnerId(String partnerId) {
        return ruleRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId)
                .stream().map(this::toRuleDomain).collect(Collectors.toList());
    }

    // --- Executions ---

    @Override
    public SplitPaymentExecution saveExecution(SplitPaymentExecution execution) {
        SplitPaymentExecutionEntity entity = toExecutionEntity(execution);
        SplitPaymentExecutionEntity saved = executionRepository.save(entity);
        return toExecutionDomain(saved);
    }

    @Override
    public Optional<SplitPaymentExecution> findExecutionById(UUID executionId) {
        return executionRepository.findById(executionId).map(this::toExecutionDomain);
    }

    @Override
    public Optional<SplitPaymentExecution> findExecutionByIdempotencyKey(String idempotencyKey) {
        return executionRepository.findByIdempotencyKey(idempotencyKey).map(this::toExecutionDomain);
    }

    @Override
    public List<SplitPaymentExecution> findExecutionsByPayerAccountId(String payerAccountId) {
        return executionRepository.findByPayerAccountIdOrderByCreatedAtDesc(payerAccountId)
                .stream().map(this::toExecutionDomain).collect(Collectors.toList());
    }

    @Override
    public List<SplitPaymentExecution> findExecutionsByStatusIn(Collection<SplitExecutionStatus> statuses) {
        List<id.payu.wallet.adapter.persistence.entity.SplitExecutionStatus> entityStatuses = statuses.stream()
                .map(status -> id.payu.wallet.adapter.persistence.entity.SplitExecutionStatus.valueOf(status.name()))
                .collect(Collectors.toList());
        return executionRepository.findByStatusIn(entityStatuses).stream()
                .map(this::toExecutionDomain).collect(Collectors.toList());
    }

    // --- Rule Mappers ---

    private SplitPaymentRuleEntity toRuleEntity(SplitPaymentRule domain) {
        SplitPaymentRuleEntity entity = new SplitPaymentRuleEntity();
        entity.setId(domain.getId());
        entity.setPartnerId(domain.getPartnerId());
        entity.setRuleName(domain.getRuleName());
        entity.setSplitType(SplitType.valueOf(domain.getSplitType().name()));
        entity.setCurrency(domain.getCurrency());
        entity.setActive(domain.isActive());

        List<SplitRecipientEntity> recipientEntities = new ArrayList<>();
        if (domain.getRecipients() != null) {
            for (SplitRecipient r : domain.getRecipients()) {
                SplitRecipientEntity re = new SplitRecipientEntity();
                re.setId(r.getId());
                re.setSplitRule(entity);
                re.setRecipientAccountId(r.getRecipientAccountId());
                re.setRecipientLabel(r.getRecipientLabel());
                re.setRecipientType(RecipientType.valueOf(r.getType().name()));
                re.setPercentage(r.getPercentage());
                re.setFixedAmount(r.getFixedAmount());
                re.setPriority(r.getPriority());
                recipientEntities.add(re);
            }
        }
        entity.setRecipients(recipientEntities);
        return entity;
    }

    private SplitPaymentRule toRuleDomain(SplitPaymentRuleEntity entity) {
        List<SplitRecipient> recipients = entity.getRecipients().stream()
                .map(re -> SplitRecipient.builder()
                        .id(re.getId())
                        .splitRuleId(entity.getId())
                        .recipientAccountId(re.getRecipientAccountId())
                        .recipientLabel(re.getRecipientLabel())
                        .type(id.payu.wallet.domain.model.RecipientType.valueOf(re.getRecipientType().name()))
                        .percentage(re.getPercentage())
                        .fixedAmount(re.getFixedAmount())
                        .priority(re.getPriority())
                        .createdAt(re.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return SplitPaymentRule.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .ruleName(entity.getRuleName())
                .splitType(id.payu.wallet.domain.model.SplitType.valueOf(entity.getSplitType().name()))
                .currency(entity.getCurrency())
                .active(entity.isActive())
                .recipients(recipients)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // --- Execution Mappers ---

    private SplitPaymentExecutionEntity toExecutionEntity(SplitPaymentExecution domain) {
        SplitPaymentExecutionEntity entity = new SplitPaymentExecutionEntity();
        entity.setId(domain.getId());
        entity.setSplitRuleId(domain.getSplitRuleId());
        entity.setPayerAccountId(domain.getPayerAccountId());
        entity.setPartnerId(domain.getPartnerId());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setExternalReferenceId(domain.getExternalReferenceId());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setStatus(id.payu.wallet.adapter.persistence.entity.SplitExecutionStatus.valueOf(domain.getStatus().name()));
        entity.setDescription(domain.getDescription());
        entity.setCompletedAt(domain.getCompletedAt());
        entity.setFailedAt(domain.getFailedAt());
        entity.setFailureReason(domain.getFailureReason());

        List<SplitPaymentLegEntity> legEntities = new ArrayList<>();
        if (domain.getLegs() != null) {
            for (SplitPaymentLeg leg : domain.getLegs()) {
                SplitPaymentLegEntity le = new SplitPaymentLegEntity();
                le.setId(leg.getId());
                le.setExecution(entity);
                le.setRecipientAccountId(leg.getRecipientAccountId());
                le.setRecipientLabel(leg.getRecipientLabel());
                le.setAmount(leg.getAmount());
                le.setStatus(LegStatus.valueOf(leg.getStatus().name()));
                le.setJournalEntryId(leg.getJournalEntryId());
                le.setSettledAt(leg.getSettledAt());
                legEntities.add(le);
            }
        }
        entity.setLegs(legEntities);
        return entity;
    }

    private SplitPaymentExecution toExecutionDomain(SplitPaymentExecutionEntity entity) {
        List<SplitPaymentLeg> legs = entity.getLegs().stream()
                .map(le -> SplitPaymentLeg.builder()
                        .id(le.getId())
                        .executionId(entity.getId())
                        .recipientAccountId(le.getRecipientAccountId())
                        .recipientLabel(le.getRecipientLabel())
                        .amount(le.getAmount())
                        .status(id.payu.wallet.domain.model.LegStatus.valueOf(le.getStatus().name()))
                        .journalEntryId(le.getJournalEntryId())
                        .settledAt(le.getSettledAt())
                        .createdAt(le.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return SplitPaymentExecution.builder()
                .id(entity.getId())
                .splitRuleId(entity.getSplitRuleId())
                .payerAccountId(entity.getPayerAccountId())
                .partnerId(entity.getPartnerId())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .externalReferenceId(entity.getExternalReferenceId())
                .idempotencyKey(entity.getIdempotencyKey())
                        .status(id.payu.wallet.domain.model.SplitExecutionStatus.valueOf(entity.getStatus().name()))
                .description(entity.getDescription())
                .legs(legs)
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .failedAt(entity.getFailedAt())
                .failureReason(entity.getFailureReason())
                .build();
    }
}
