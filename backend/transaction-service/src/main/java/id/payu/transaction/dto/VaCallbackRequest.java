package id.payu.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for VA bank callback (payment confirmation).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaCallbackRequest {

    @NotBlank(message = "VA number is required")
    private String vaNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank(message = "Payment reference is required")
    private String paymentReference;

    private String bankReferenceNumber;
}
