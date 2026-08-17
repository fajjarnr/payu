package id.payu.integration.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for SWIFT message processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SWIFT message processing request")
public class SwiftMessageRequest {

    @NotBlank(message = "SWIFT message is required")
    @Schema(description = "Raw SWIFT MT message", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "{1:F01PAYUIDJAAXXX0000000000}{2:I103PAYUIDJAXXXXN}{4:\n:20:REF123456\n:23B:CRED\n:32A:240101IDR1000000,00\n:50K:/1234567890\nJOHN DOE\n:59:/9876543210\nJANE SMITH\n:71A:SHA\n-}{5:{CHK:000000000000}}")
    private String swiftMessage;

    @Schema(description = "SWIFT message type (auto-detected if not provided)", example = "MT103")
    private String messageType;

    @Schema(description = "Correlation ID for tracking", example = "corr-123456")
    private String correlationId;
}
