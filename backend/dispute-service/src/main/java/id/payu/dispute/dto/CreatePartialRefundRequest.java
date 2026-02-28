package id.payu.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a partial refund.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a partial refund")
public class CreatePartialRefundRequest {

    @NotNull(message = "Transaction ID is required")
    @Schema(description = "Transaction ID to refund", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID transactionId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(description = "Amount to refund", example = "50000.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Schema(description = "Currency code", example = "IDR")
    private String currency;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for refund", example = "Partial refund for damaged item")
    private String reason;
}
