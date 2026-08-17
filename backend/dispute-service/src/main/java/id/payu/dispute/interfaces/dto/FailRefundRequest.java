package id.payu.dispute.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for failing a refund.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to fail a refund")
public class FailRefundRequest {

    @NotBlank(message = "Failure reason is required")
    @Schema(description = "Reason for failure", example = "Insufficient funds")
    private String failureReason;
}
