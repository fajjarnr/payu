package id.payu.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for dispute information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dispute information")
public class DisputeResponse {

    @Schema(description = "Dispute ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID id;

    @Schema(description = "Transaction ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID transactionId;

    @Schema(description = "Customer ID", example = "660e8400-e29b-41d4-a716-446655440001")
    private UUID customerId;

    @Schema(description = "Merchant ID", example = "770e8400-e29b-41d4-a716-446655440002")
    private UUID merchantId;

    @Schema(description = "Disputed amount", example = "100000.00")
    private BigDecimal disputedAmount;

    @Schema(description = "Currency code", example = "IDR")
    private String currency;

    @Schema(description = "Reason for dispute", example = "Product not received")
    private String reason;

    @Schema(description = "Dispute status", example = "OPEN")
    private String status;

    @Schema(description = "Investigation ID")
    private String investigationId;

    @Schema(description = "Resolution type")
    private String resolutionType;

    @Schema(description = "Resolution description")
    private String resolution;

    @Schema(description = "Rejection reason")
    private String rejectionReason;

    @Schema(description = "Escalation reason")
    private String escalationReason;

    @Schema(description = "Open timestamp")
    private Instant openedAt;

    @Schema(description = "Investigation start timestamp")
    private Instant investigationStartedAt;

    @Schema(description = "Resolution timestamp")
    private Instant resolvedAt;

    @Schema(description = "Rejection timestamp")
    private Instant rejectedAt;

    @Schema(description = "Escalation timestamp")
    private Instant escalatedAt;

    @Schema(description = "List of evidence")
    private List<DisputeEvidenceResponse> evidenceList;
}
