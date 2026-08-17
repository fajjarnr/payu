package id.payu.integration.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for HTTP request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "HTTP request")
public class HttpRequest {

    @NotBlank(message = "URL is required")
    @Schema(description = "Target URL", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://api.external-system.com/v1/data")
    private String url;

    @NotBlank(message = "HTTP method is required")
    @Schema(description = "HTTP method", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "POST", allowableValues = {"GET", "POST", "PUT", "DELETE", "PATCH"})
    private String method;

    @Schema(description = "HTTP headers")
    private Map<String, String> headers;

    @Schema(description = "Request body")
    private String body;
}
