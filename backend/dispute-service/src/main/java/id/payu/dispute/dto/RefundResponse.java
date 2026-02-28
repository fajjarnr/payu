package id.payu.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for refund information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refund information")
public class RefundResponse {

    @Schema(description = "Refund ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID id;

    @Schema(description = "Transaction ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID transactionId;

    @Schema(description = "Refund amount", example = "100000.00")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "IDR")
    private String currency;

    @Schema(description = "Reason for refund", example = "Customer request")
    private String reason;

    @Schema(description = "Refund status", example = "PENDING")
    private String status;

    @Schema(description = "Failure reason if refund failed")
    private String failureReason;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Processing timestamp")
    private Instant processedAt;

    @Schema(description = "Completion timestamp")
    private Instant completedAt;

    @Schema(description = "Failure timestamp")
    private Instant failedAt;

    @Schema(description = "Cancellation timestamp")
    private Instant cancelledAt;
}
