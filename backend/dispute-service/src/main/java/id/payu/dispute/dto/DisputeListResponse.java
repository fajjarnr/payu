package id.payu.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for list of disputes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "List of disputes")
public class DisputeListResponse {

    @Schema(description = "List of disputes")
    private List<DisputeResponse> disputes;

    @Schema(description = "Total number of disputes", example = "5")
    private int total;
}
