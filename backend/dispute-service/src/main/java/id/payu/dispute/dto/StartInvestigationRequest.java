package id.payu.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting investigation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to start investigation")
public class StartInvestigationRequest {

    @NotBlank(message = "Investigation ID is required")
    @Schema(description = "Investigation identifier", example = "INV-001")
    private String investigationId;
}
