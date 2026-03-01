package id.payu.integration.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for SOAP request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SOAP request")
public class SoapRequest {

    @NotBlank(message = "Endpoint URL is required")
    @Schema(description = "SOAP endpoint URL", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://legacy-system.bank.com/soap/v1/Service")
    private String endpoint;

    @NotBlank(message = "Operation name is required")
    @Schema(description = "SOAP operation name", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "GetAccountBalance")
    private String operation;

    @NotBlank(message = "Payload is required")
    @Schema(description = "SOAP payload (body content, will be wrapped in envelope)", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "<accountNumber>1234567890</accountNumber>")
    private String payload;

    @Schema(description = "SOAP version", example = "1.1", allowableValues = {"1.1", "1.2"})
    private String soapVersion;

    @Schema(description = "Additional HTTP headers")
    private Map<String, String> headers;
}
