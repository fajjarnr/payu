package id.payu.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Error information included in failed API responses.
 * Follows PayU's centralized error code management pattern.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error information for failed requests")
public class ErrorInfo {

    @Schema(
            description = "Unique error code for client-side logic and localization",
            example = "INSUFFICIENT_BALANCE"
    )
    private String code;

    @Schema(
            description = "Human-readable error message (for convenience, may be localized)",
            example = "Saldo tidak mencukupi untuk transaksi ini"
    )
    private String message;

    @Schema(
            description = "Detailed field-level validation errors"
    )
    private List<FieldError> details;

    /**
     * Creates ErrorInfo with code and message.
     */
    public static ErrorInfo of(String code, String message) {
        return ErrorInfo.builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * Creates ErrorInfo with code, message, and details.
     */
    public static ErrorInfo of(String code, String message, List<FieldError> details) {
        return ErrorInfo.builder()
                .code(code)
                .message(message)
                .details(details)
                .build();
    }
}
