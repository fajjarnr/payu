package id.payu.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding evidence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add evidence to a dispute")
public class AddEvidenceRequest {

    @NotBlank(message = "File name is required")
    @Schema(description = "Evidence file name", example = "receipt.pdf")
    private String fileName;

    @NotBlank(message = "File URL is required")
    @Schema(description = "Evidence file URL", example = "https://storage.payu.fajjjar.my.id/evidence/receipt.pdf")
    private String fileUrl;

    @NotBlank(message = "Uploaded by is required")
    @Schema(description = "Who uploaded the evidence (CUSTOMER, MERCHANT, SYSTEM)", example = "CUSTOMER")
    private String uploadedBy;
}
