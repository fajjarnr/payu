package id.payu.dispute.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateChargebackRequest {
    @NotNull
    private UUID transactionId;
    @NotNull
    private UUID customerId;
    @NotNull
    private UUID merchantId;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private String currency;
    @NotNull
    private String reason;
}
