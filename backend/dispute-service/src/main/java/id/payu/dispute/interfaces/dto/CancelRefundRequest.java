package id.payu.dispute.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for cancelling a refund.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel a refund")
public class CancelRefundRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Schema(description = "Reason for cancellation", example = "Customer changed mind")
    private String cancellationReason;
}
