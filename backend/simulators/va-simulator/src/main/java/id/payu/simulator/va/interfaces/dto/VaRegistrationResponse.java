package id.payu.simulator.va.interfaces.dto;

import java.math.BigDecimal;

/**
 * Response to VA registration request.
 */
public record VaRegistrationResponse(
    String responseCode,
    String responseMessage,
    String vaNumber,
    String bankCode,
    String bankName,
    BigDecimal amount,
    String currency,
    String expiresAt,
    String status
) {}
