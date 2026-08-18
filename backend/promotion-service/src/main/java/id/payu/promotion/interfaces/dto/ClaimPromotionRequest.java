package id.payu.promotion.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ClaimPromotionRequest(
    @NotBlank String accountId,
    @NotBlank String transactionId,
    @NotNull BigDecimal transactionAmount,
    String merchantCode,
    String categoryCode
) {}
