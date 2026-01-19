package id.payu.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessQrisPaymentRequest {
    @NotBlank(message = "QRIS code is required")
    private String qrisCode;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency;

    @NotNull(message = "Customer account ID is required")
    private Long customerId;

    private String transactionPin;
    private String deviceId;
}
