package id.payu.lending.dto;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.domain.model.PreApprovalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.lending.domain.model.LoanType;
import id.payu.lending.domain.model.RiskCategory;

public record LoanPreApprovalResponse(
        UUID preApprovalId,
        UUID userId,
        LoanType loanType,
        BigDecimal principalAmount,
        BigDecimal maxApprovedAmount,
        BigDecimal minInterestRate,
        Integer maxTenureMonths,
        BigDecimal estimatedMonthlyPayment,
        PreApprovalStatus status,
        BigDecimal creditScore,
        RiskCategory riskCategory,
        String reason,
        LocalDateTime validUntil,
        LocalDateTime createdAt
) {
}
