package id.payu.dispute.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for rejecting a dispute.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to reject a dispute")
public class RejectDisputeRequest {

    @NotBlank(message = "Rejection reason is required")
    @Schema(description = "Reason for rejection", example = "Dispute filed after deadline")
    private String rejectionReason;
}
