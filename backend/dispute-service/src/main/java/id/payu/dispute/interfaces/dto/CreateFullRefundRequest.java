package id.payu.dispute.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a full refund.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a full refund")
public class CreateFullRefundRequest {

    @NotNull(message = "Transaction ID is required")
    @Schema(description = "Transaction ID to refund", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID transactionId;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for refund", example = "Customer request")
    private String reason;
}
