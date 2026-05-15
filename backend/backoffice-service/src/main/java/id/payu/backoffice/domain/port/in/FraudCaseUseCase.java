package id.payu.backoffice.domain.port.in;

import id.payu.backoffice.domain.FraudCase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for Fraud Case use cases.
 */
public interface FraudCaseUseCase {

    FraudCase create(String userId, String accountNumber, UUID transactionId,
                     String transactionType, BigDecimal amount, String fraudType,
                     FraudCase.RiskLevel riskLevel, String description, String evidence);

    Optional<FraudCase> getById(UUID id);

    List<FraudCase> getByUserId(String userId);

    List<FraudCase> listByStatus(FraudCase.CaseStatus status, int page, int size);

    List<FraudCase> listByRiskLevel(FraudCase.RiskLevel riskLevel, int page, int size);

    List<FraudCase> listAll(int page, int size);

    FraudCase assign(UUID id, String assignedTo);

    FraudCase resolve(UUID id, FraudCase.CaseStatus newStatus, String notes, String resolvedBy);

    void delete(UUID id);
}
