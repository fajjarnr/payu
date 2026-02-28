package id.payu.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for resolving a dispute.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to resolve a dispute")
public class ResolveDisputeRequest {

    @NotBlank(message = "Resolution type is required")
    @Schema(description = "Resolution type (REFUND_CUSTOMER, REJECT_CLAIM, PARTIAL_REFUND)", example = "REFUND_CUSTOMER")
    private String resolutionType;

    @NotBlank(message = "Resolution description is required")
    @Schema(description = "Resolution description", example = "Evidence supports customer claim")
    private String resolution;
}
