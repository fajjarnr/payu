package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for BI-FAST disbursement callback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementCallbackRequest {

    @NotNull(message = "Disbursement ID is required")
    private UUID disbursementId;

    @NotBlank(message = "Status is required")
    private String status; // COMPLETED or FAILED

    private String bankReference;

    private String failureReason;
}
