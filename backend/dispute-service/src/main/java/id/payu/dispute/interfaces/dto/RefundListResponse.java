package id.payu.dispute.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for list of refunds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "List of refunds")
public class RefundListResponse {

    @Schema(description = "List of refunds")
    private List<RefundResponse> refunds;

    @Schema(description = "Total number of refunds", example = "5")
    private int total;
}
