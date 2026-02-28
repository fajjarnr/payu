package id.payu.transaction.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class P2PTransferRequest {

    @NotNull(message = "Sender account ID is required")
    private UUID senderAccountId;

    @NotBlank(message = "Destination phone number is required")
    @Pattern(regexp = "^08[0-9]{8,12}$", message = "Invalid Indonesian phone number format")
    private String destinationPhone;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Sensitive
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    @Size(max = 140, message = "Memo must not exceed 140 characters")
    private String memo;

    @Size(min = 6, max = 6, message = "Transaction PIN must be exactly 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "Transaction PIN must be exactly 6 digits")
    @Sensitive(value = Sensitive.SensitivityLevel.CRITICAL)
    private String transactionPin;

    @Size(max = 100, message = "Idempotency key is too long")
    private String idempotencyKey;
}
