package id.payu.backoffice.dto;

import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;
import id.payu.backoffice.domain.FraudCaseStatus;
import id.payu.backoffice.domain.RiskLevel;

public record FraudCaseResponse(
        UUID id,
        String userId,
        String accountNumber,
        UUID transactionId,
        String transactionType,
        BigDecimal amount,
        String fraudType,
        RiskLevel riskLevel,
        FraudCaseStatus status,
        String description,
        String evidence,
        String notes,
        String assignedTo,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static FraudCaseResponse from(FraudCaseEntity fraudCase) {
        return new FraudCaseResponse(
                fraudCase.getId(),
                fraudCase.getUserId(),
                fraudCase.getAccountNumber(),
                fraudCase.getTransactionId(),
                fraudCase.getTransactionType(),
                fraudCase.getAmount(),
                fraudCase.getFraudType(),
                fraudCase.getRiskLevel(),
                fraudCase.getStatus(),
                fraudCase.getDescription(),
                fraudCase.getEvidence(),
                fraudCase.getNotes(),
                fraudCase.getAssignedTo(),
                fraudCase.getResolvedBy(),
                fraudCase.getResolvedAt(),
                fraudCase.getCreatedAt()
        );
    }
}
