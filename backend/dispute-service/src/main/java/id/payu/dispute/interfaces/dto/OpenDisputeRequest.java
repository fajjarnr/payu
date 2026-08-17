package id.payu.dispute.interfaces.dto;

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
 * Request DTO for opening a dispute.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to open a dispute")
public class OpenDisputeRequest {

    @NotNull(message = "Transaction ID is required")
    @Schema(description = "Transaction ID being disputed", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID transactionId;

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Customer ID opening the dispute", example = "660e8400-e29b-41d4-a716-446655440001")
    private UUID customerId;

    @NotNull(message = "Merchant ID is required")
    @Schema(description = "Merchant ID involved in dispute", example = "770e8400-e29b-41d4-a716-446655440002")
    private UUID merchantId;

    @NotNull(message = "Disputed amount is required")
    @Positive(message = "Disputed amount must be positive")
    @Schema(description = "Amount being disputed", example = "100000.00")
    private BigDecimal disputedAmount;

    @NotBlank(message = "Currency is required")
    @Schema(description = "Currency code", example = "IDR")
    private String currency;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for dispute", example = "Product not received")
    private String reason;
}
