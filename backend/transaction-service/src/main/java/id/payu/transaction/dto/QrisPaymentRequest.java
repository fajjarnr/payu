package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrisPaymentRequest {
    @NotBlank(message = "QRIS code is required")
    private String qrisCode;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @NotBlank(message = "Customer reference is required")
    private String customerReference;
}
