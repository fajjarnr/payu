package id.payu.backoffice.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * BUG-BE-158 FIX: JSON request body DTO for creating fraud cases.
 * Replaces form-encoded @RequestParam pattern for consistency with all other endpoints.
 */
public record CreateFraudCaseRequest(
        @NotBlank(message = "userId is required")
        String userId,

        String accountNumber,

        UUID transactionId,

        String transactionType,

        BigDecimal amount,

        @NotBlank(message = "fraudType is required")
        String fraudType,

        String riskLevel,

        String description,

        String evidence
) {}
