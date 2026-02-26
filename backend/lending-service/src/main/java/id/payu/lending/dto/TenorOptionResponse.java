package id.payu.lending.dto;

import id.payu.lending.domain.model.InstallmentOption;

import java.math.BigDecimal;

public record TenorOptionResponse(
        int tenor,
        BigDecimal monthlyPayment,
        BigDecimal totalPayment,
        BigDecimal totalInterest,
        BigDecimal interestRate
) {
    public static TenorOptionResponse from(InstallmentOption option) {
        return new TenorOptionResponse(
                option.getTenor(),
                option.getMonthlyPayment(),
                option.getTotalPayment(),
                option.getTotalInterest(),
                option.getInterestRate()
        );
    }
}
