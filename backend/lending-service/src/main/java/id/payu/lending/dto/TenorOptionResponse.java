package id.payu.lending.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import id.payu.lending.domain.model.InstallmentOption;

import java.math.BigDecimal;

public record TenorOptionResponse(
        int tenor,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal monthlyPayment,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal totalPayment,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal totalInterest,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal interestRate
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
