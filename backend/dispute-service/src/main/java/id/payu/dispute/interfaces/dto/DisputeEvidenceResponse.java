package id.payu.dispute.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for dispute evidence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dispute evidence information")
public class DisputeEvidenceResponse {

    @Schema(description = "Evidence ID", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID id;

    @Schema(description = "File name", example = "receipt.pdf")
    private String fileName;

    @Schema(description = "File URL", example = "https://storage.payu.fajjjar.my.id/evidence/receipt.pdf")
    private String fileUrl;

    @Schema(description = "Uploaded by", example = "CUSTOMER")
    private String uploadedBy;

    @Schema(description = "Upload timestamp")
    private Instant uploadedAt;
}
