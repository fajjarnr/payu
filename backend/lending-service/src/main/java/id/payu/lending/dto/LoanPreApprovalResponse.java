package id.payu.lending.dto;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.CreditScore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanPreApprovalResponse(
        UUID preApprovalId,
        UUID userId,
        Loan.LoanType loanType,
        BigDecimal principalAmount,
        BigDecimal maxApprovedAmount,
        BigDecimal minInterestRate,
        Integer maxTenureMonths,
        BigDecimal estimatedMonthlyPayment,
        PreApprovalStatus status,
        BigDecimal creditScore,
        CreditScore.RiskCategory riskCategory,
        String reason,
        LocalDateTime validUntil,
        LocalDateTime createdAt
) {

    public enum PreApprovalStatus {
        APPROVED,
        CONDITIONALLY_APPROVED,
        REJECTED
    }
}
