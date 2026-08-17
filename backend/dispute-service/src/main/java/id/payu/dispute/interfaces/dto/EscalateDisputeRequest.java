package id.payu.dispute.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for escalating a dispute.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to escalate a dispute")
public class EscalateDisputeRequest {

    @NotBlank(message = "Escalation reason is required")
    @Schema(description = "Reason for escalation", example = "Requires senior review")
    private String escalationReason;
}
