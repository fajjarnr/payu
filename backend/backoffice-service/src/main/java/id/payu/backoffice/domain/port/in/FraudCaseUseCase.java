package id.payu.backoffice.domain.port.in;

import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.FraudCaseStatus;
import id.payu.backoffice.domain.RiskLevel;

/**
 * Inbound port for Fraud Case use cases.
 */
public interface FraudCaseUseCase {

    FraudCaseEntity create(String userId, String accountNumber, UUID transactionId,
                     String transactionType, BigDecimal amount, String fraudType,
                     RiskLevel riskLevel, String description, String evidence);

    Optional<FraudCaseEntity> getById(UUID id);

    List<FraudCaseEntity> getByUserId(String userId);

    List<FraudCaseEntity> listByStatus(FraudCaseStatus status, int page, int size);

    List<FraudCaseEntity> listByRiskLevel(RiskLevel riskLevel, int page, int size);

    List<FraudCaseEntity> listAll(int page, int size);

    FraudCaseEntity assign(UUID id, String assignedTo);

    FraudCaseEntity resolve(UUID id, FraudCaseStatus newStatus, String notes, String resolvedBy);

    void delete(UUID id);
}
