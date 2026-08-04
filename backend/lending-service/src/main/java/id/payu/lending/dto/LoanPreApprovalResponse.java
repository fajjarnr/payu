package id.payu.lending.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal principalAmount,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal maxApprovedAmount,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal minInterestRate,
        Integer maxTenureMonths,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal estimatedMonthlyPayment,
        PreApprovalStatus status,
        BigDecimal creditScore,
        RiskCategory riskCategory,
        String reason,
        LocalDateTime validUntil,
        LocalDateTime createdAt
) {
}
