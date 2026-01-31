package id.payu.backoffice.dto;

import id.payu.backoffice.domain.FraudCase;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

public record FraudCaseResponse(
        UUID id,
        String userId,
        String accountNumber,
        UUID transactionId,
        String transactionType,
        BigDecimal amount,
        String fraudType,
        FraudCase.RiskLevel riskLevel,
        FraudCase.CaseStatus status,
        String description,
        String evidence,
        String notes,
        String assignedTo,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static FraudCaseResponse from(FraudCase fraudCase) {
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
