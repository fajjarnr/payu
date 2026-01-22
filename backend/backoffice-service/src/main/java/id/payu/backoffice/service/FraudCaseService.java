package id.payu.backoffice.service;

import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.dto.FraudCaseDecisionRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FraudCaseService {

    private static final Logger LOG = Logger.getLogger(FraudCaseService.class);

    @Transactional
    public FraudCase create(String userId, String accountNumber, UUID transactionId, 
                           String transactionType, BigDecimal amount, String fraudType, 
                           FraudCase.RiskLevel riskLevel, String description, String evidence) {
        LOG.infof("Creating fraud case for user: %s, transaction: %s", userId, transactionId);

        FraudCase fraudCase = new FraudCase();
        fraudCase.userId = userId;
        fraudCase.accountNumber = accountNumber;
        fraudCase.transactionId = transactionId;
        fraudCase.transactionType = transactionType;
        fraudCase.amount = amount;
        fraudCase.fraudType = fraudType;
        fraudCase.riskLevel = riskLevel != null ? riskLevel : FraudCase.RiskLevel.MEDIUM;
        fraudCase.status = FraudCase.CaseStatus.OPEN;
        fraudCase.description = description;
        fraudCase.evidence = evidence;

        fraudCase.persist();
        LOG.infof("Fraud case created: id=%s", fraudCase.id);
        return fraudCase;
    }

    public Optional<FraudCase> getById(UUID id) {
        return FraudCase.findByIdOptional(id);
    }

    public List<FraudCase> getByUserId(String userId) {
        return FraudCase.<FraudCase>find("userId = ?1 ORDER BY createdAt DESC", userId).list();
    }

    public List<FraudCase> listByStatus(FraudCase.CaseStatus status, int page, int size) {
        return FraudCase.<FraudCase>find("status = ?1 ORDER BY createdAt DESC", status)
                .page(page, size)
                .list();
    }

    public List<FraudCase> listByRiskLevel(FraudCase.RiskLevel riskLevel, int page, int size) {
        return FraudCase.<FraudCase>find("riskLevel = ?1 ORDER BY createdAt DESC", riskLevel)
                .page(page, size)
                .list();
    }

    public List<FraudCase> listAll(int page, int size) {
        return FraudCase.<FraudCase>findAll()
                .page(page, size)
                .list();
    }

    @Transactional
    public FraudCase assign(UUID id, String assignedTo) {
        LOG.infof("Assigning fraud case: id=%s, to=%s", id, assignedTo);

        FraudCase fraudCase = FraudCase.<FraudCase>findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + id));

        fraudCase.assignedTo = assignedTo;
        if (fraudCase.status == FraudCase.CaseStatus.OPEN) {
            fraudCase.status = FraudCase.CaseStatus.UNDER_INVESTIGATION;
        }

        fraudCase.persist();
        return fraudCase;
    }

    @Transactional
    public FraudCase resolve(UUID id, FraudCaseDecisionRequest request, String resolvedBy) {
        LOG.infof("Resolving fraud case: id=%s, status=%s, resolver=%s", id, request.status(), resolvedBy);

        FraudCase fraudCase = FraudCase.<FraudCase>findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + id));

        fraudCase.status = switch (request.status()) {
            case UNDER_INVESTIGATION -> FraudCase.CaseStatus.UNDER_INVESTIGATION;
            case RESOLVED -> FraudCase.CaseStatus.RESOLVED;
            case CLOSED -> FraudCase.CaseStatus.CLOSED;
            case ESCALATED -> FraudCase.CaseStatus.ESCALATED;
        };

        fraudCase.notes = request.notes();
        fraudCase.resolvedBy = resolvedBy;
        fraudCase.resolvedAt = LocalDateTime.now();

        fraudCase.persist();
        LOG.infof("Fraud case updated: id=%s, newStatus=%s", fraudCase.id, fraudCase.status);
        return fraudCase;
    }

    @Transactional
    public void delete(UUID id) {
        LOG.infof("Deleting fraud case: id=%s", id);
        FraudCase.deleteById(id);
    }
}
