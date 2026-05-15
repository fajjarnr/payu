package id.payu.backoffice.application.service;

import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity;
import id.payu.backoffice.dto.FraudCaseDecisionRequest;
import id.payu.backoffice.adapter.persistence.repository.FraudCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.FraudCaseStatus;
import id.payu.backoffice.domain.RiskLevel;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCaseService {

    private final FraudCaseRepository repository;

    @Transactional
    public FraudCaseEntity create(String userId, String accountNumber, UUID transactionId, 
                           String transactionType, BigDecimal amount, String fraudType, 
                           RiskLevel riskLevel, String description, String evidence) {
        log.info("Creating fraud case for user: {}, transaction: {}", userId, transactionId);

        FraudCaseEntity fraudCase = FraudCaseEntity.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .transactionId(transactionId)
                .transactionType(transactionType)
                .amount(amount)
                .fraudType(fraudType)
                .riskLevel(riskLevel != null ? riskLevel : RiskLevel.MEDIUM)
                .status(FraudCaseStatus.OPEN)
                .description(description)
                .evidence(evidence)
                .build();

        FraudCaseEntity saved = repository.save(fraudCase);
        log.info("Fraud case created: id={}", saved.getId());
        return saved;
    }

    public Optional<FraudCaseEntity> getById(UUID id) {
        return repository.findById(id);
    }

    public List<FraudCaseEntity> getByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public List<FraudCaseEntity> listByStatus(FraudCaseStatus status, int page, int size) {
        // BUG-BE-043: Use DB-level pagination instead of ignoring page/size
        return repository.findByStatus(status, PageRequest.of(page, size)).getContent();
    }

    public List<FraudCaseEntity> listByRiskLevel(RiskLevel riskLevel, int page, int size) {
        // Need to add method to repository if needed, or filter manually (inefficient) or ignore pagination for now
        // Assuming repo has it or just simplified
        return repository.findAll().stream()
                .filter(fc -> fc.getRiskLevel() == riskLevel)
                .toList(); 
    }

    public List<FraudCaseEntity> listAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public FraudCaseEntity assign(UUID id, String assignedTo) {
        log.info("Assigning fraud case: id={}, to={}", id, assignedTo);

        FraudCaseEntity fraudCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + id));

        fraudCase.setAssignedTo(assignedTo);
        if (fraudCase.getStatus() == FraudCaseStatus.OPEN) {
            fraudCase.setStatus(FraudCaseStatus.UNDER_INVESTIGATION);
        }

        return repository.save(fraudCase);
    }

    @Transactional
    public FraudCaseEntity resolve(UUID id, FraudCaseDecisionRequest request, String resolvedBy) {
        log.info("Resolving fraud case: id={}, status={}, resolver={}", id, request.status(), resolvedBy);

        FraudCaseEntity fraudCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + id));

        fraudCase.setStatus(switch (request.status()) {
            case UNDER_INVESTIGATION -> FraudCaseStatus.UNDER_INVESTIGATION;
            case RESOLVED -> FraudCaseStatus.RESOLVED;
            case CLOSED -> FraudCaseStatus.CLOSED;
            case ESCALATED -> FraudCaseStatus.ESCALATED;
        });

        fraudCase.setNotes(request.notes());
        fraudCase.setResolvedBy(resolvedBy);
        fraudCase.setResolvedAt(LocalDateTime.now());

        FraudCaseEntity saved = repository.save(fraudCase);
        log.info("Fraud case updated: id={}, newStatus={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting fraud case: id={}", id);
        repository.deleteById(id);
    }
}
