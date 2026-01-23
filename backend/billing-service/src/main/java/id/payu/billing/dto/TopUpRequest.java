package id.payu.billing.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TopUpRequest(
    @NotBlank(message = "Account ID is required")
    String accountId,

    @NotBlank(message = "E-wallet provider is required")
    @Pattern(regexp = "GOPAY|OVO|DANA|LINKAJA", message = "Invalid e-wallet provider. Must be GOPAY, OVO, DANA, or LINKAJA")
    String provider,

    @NotBlank(message = "E-wallet number is required")
    @Size(min = 10, max = 14, message = "E-wallet number must be between 10 and 14 digits")
    String walletNumber,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "10000.00", message = "Minimum top-up amount is Rp 10.000")
    @DecimalMax(value = "2000000.00", message = "Maximum top-up amount is Rp 2.000.000")
    BigDecimal amount
) {}