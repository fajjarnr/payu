package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.FraudCase;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCaseService {

    private final FraudCaseRepository repository;

    @Transactional
    public FraudCase create(String userId, String accountNumber, UUID transactionId, 
                           String transactionType, BigDecimal amount, String fraudType, 
                           FraudCase.RiskLevel riskLevel, String description, String evidence) {
        log.info("Creating fraud case for user: {}, transaction: {}", userId, transactionId);

        FraudCase fraudCase = FraudCase.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .transactionId(transactionId)
                .transactionType(transactionType)
                .amount(amount)
                .fraudType(fraudType)
                .riskLevel(riskLevel != null ? riskLevel : FraudCase.RiskLevel.MEDIUM)
                .status(FraudCase.CaseStatus.OPEN)
                .description(description)
                .evidence(evidence)
                .build();

        FraudCase saved = repository.save(fraudCase);
        log.info("Fraud case created: id={}", saved.getId());
        return saved;
    }

    public Optional<FraudCase> getById(UUID id) {
        return repository.findById(id);
    }

    public List<FraudCase> getByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public List<FraudCase> listByStatus(FraudCase.CaseStatus status, int page, int size) {
        return repository.findByStatus(status);
    }

    public List<FraudCase> listByRiskLevel(FraudCase.RiskLevel riskLevel, int page, int size) {
        // Need to add method to repository if needed, or filter manually (inefficient) or ignore pagination for now
        // Assuming repo has it or just simplified
        return repository.findAll().stream()
                .filter(fc -> fc.getRiskLevel() == riskLevel)
                .toList(); 
    }

    public List<FraudCase> listAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public FraudCase assign(UUID id, String assignedTo) {
        log.info("Assigning fraud case: id={}, to={}", id, assignedTo);

        FraudCase fraudCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + id));

        fraudCase.setAssignedTo(assignedTo);
        if (fraudCase.getStatus() == FraudCase.CaseStatus.OPEN) {
            fraudCase.setStatus(FraudCase.CaseStatus.UNDER_INVESTIGATION);
        }

        return repository.save(fraudCase);
    }

    @Transactional
    public FraudCase resolve(UUID id, FraudCaseDecisionRequest request, String resolvedBy) {
        log.info("Resolving fraud case: id={}, status={}, resolver={}", id, request.status(), resolvedBy);

        FraudCase fraudCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + id));

        fraudCase.setStatus(switch (request.status()) {
            case UNDER_INVESTIGATION -> FraudCase.CaseStatus.UNDER_INVESTIGATION;
            case RESOLVED -> FraudCase.CaseStatus.RESOLVED;
            case CLOSED -> FraudCase.CaseStatus.CLOSED;
            case ESCALATED -> FraudCase.CaseStatus.ESCALATED;
        });

        fraudCase.setNotes(request.notes());
        fraudCase.setResolvedBy(resolvedBy);
        fraudCase.setResolvedAt(LocalDateTime.now());

        FraudCase saved = repository.save(fraudCase);
        log.info("Fraud case updated: id={}, newStatus={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting fraud case: id={}", id);
        repository.deleteById(id);
    }
}
