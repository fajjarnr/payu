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
                fraudCase.id,
                fraudCase.userId,
                fraudCase.accountNumber,
                fraudCase.transactionId,
                fraudCase.transactionType,
                fraudCase.amount,
                fraudCase.fraudType,
                fraudCase.riskLevel,
                fraudCase.status,
                fraudCase.description,
                fraudCase.evidence,
                fraudCase.notes,
                fraudCase.assignedTo,
                fraudCase.resolvedBy,
                fraudCase.resolvedAt,
                fraudCase.createdAt
        );
    }
}
