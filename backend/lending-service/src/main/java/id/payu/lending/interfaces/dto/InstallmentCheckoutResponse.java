package id.payu.lending.interfaces.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import id.payu.lending.domain.model.InstallmentCheckout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InstallmentCheckoutResponse(
        UUID id,
        UUID userId,
        UUID payLaterId,
        UUID loanId,
        String partnerId,
        String externalOrderId,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal purchaseAmount,
        String currency,
        int tenor,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal monthlyPayment,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal interestRate,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InstallmentCheckoutResponse from(InstallmentCheckout checkout) {
        return new InstallmentCheckoutResponse(
                checkout.getId(),
                checkout.getUserId(),
                checkout.getPayLaterId(),
                checkout.getLoanId(),
                checkout.getPartnerId(),
                checkout.getExternalOrderId(),
                checkout.getPurchaseAmount(),
                checkout.getCurrency(),
                checkout.getTenor(),
                checkout.getMonthlyPayment(),
                checkout.getInterestRate(),
                checkout.getStatus().name(),
                checkout.getFailureReason(),
                checkout.getCreatedAt(),
                checkout.getUpdatedAt()
        );
    }
}
