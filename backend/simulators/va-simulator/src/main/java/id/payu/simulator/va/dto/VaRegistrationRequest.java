package id.payu.simulator.va.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request to register a Virtual Account with the simulator.
 * Called by PayU when creating a new VA.
 */
public record VaRegistrationRequest(
    @NotBlank(message = "VA number is required")
    String vaNumber,

    @NotBlank(message = "Bank code is required")
    String bankCode,

    String bankName,

    @NotBlank(message = "Partner ID is required")
    String partnerId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    String currency,

    @NotNull(message = "Expiry time is required")
    Instant expiresAt,

    String callbackUrl,

    String externalId,

    String customerName,

    String description
) {}
