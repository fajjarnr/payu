package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.interfaces.dto.FraudCaseDecisionRequest;
import id.payu.backoffice.domain.port.outbound.FraudCaseRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.FraudCaseStatus;
import id.payu.backoffice.domain.RiskLevel;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCaseService {

    private final FraudCaseRepositoryPort repository;

    @Transactional
    public FraudCase create(String userId, String accountNumber, UUID transactionId,
                           String transactionType, BigDecimal amount, String fraudType,
                           RiskLevel riskLevel, String description, String evidence) {
        log.info("Creating fraud case: transaction={}", transactionId);
        FraudCase saved = repository.save(FraudCase.create(userId, accountNumber, transactionId,
                transactionType, amount, fraudType, riskLevel, description, evidence));
        log.info("Fraud case created: id={}", saved.getId());
        return saved;
    }

    public Optional<FraudCase> getById(UUID id) {
        return repository.findById(id);
    }

    public List<FraudCase> getByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public List<FraudCase> listByStatus(FraudCaseStatus status, int page, int size) {
        return repository.findByStatus(status, page, size);
    }

    public List<FraudCase> listByRiskLevel(RiskLevel riskLevel, int page, int size) {
        return repository.findByRiskLevel(riskLevel, page, size);
    }

    public List<FraudCase> listAll(int page, int size) {
        return repository.findAll(page, size);
    }

    @Transactional
    public FraudCase assign(UUID id, String assignedTo) {
        log.info("Assigning fraud case: id={}", id);

        FraudCase fraudCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + id));

        fraudCase.assignTo(assignedTo);

        return repository.save(fraudCase);
    }

    @Transactional
    public FraudCase resolve(UUID id, FraudCaseDecisionRequest request, String resolvedBy) {
        log.info("Resolving fraud case: id={}, status={}", id, request.status());

        FraudCase fraudCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + id));

        FraudCaseStatus status = switch (request.status()) {
            case UNDER_INVESTIGATION -> FraudCaseStatus.UNDER_INVESTIGATION;
            case RESOLVED -> FraudCaseStatus.RESOLVED;
            case CLOSED -> FraudCaseStatus.CLOSED;
            case ESCALATED -> FraudCaseStatus.ESCALATED;
        };
        fraudCase.resolve(status, request.notes(), resolvedBy);
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
