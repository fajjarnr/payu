package id.payu.partner.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Error information included in failed API responses.
 * Follows PayU's centralized error code management pattern.
 */
@Schema(description = "Error information for failed requests")
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    public ErrorInfo() {
    }

    public ErrorInfo(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorInfo(String code, String message, List<FieldError> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public static ErrorInfo of(String code, String message) {
        return new ErrorInfo(code, message);
    }

    public static ErrorInfo of(String code, String message, List<FieldError> details) {
        return new ErrorInfo(code, message, details);
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FieldError> getDetails() {
        return details;
    }

    public void setDetails(List<FieldError> details) {
        this.details = details;
    }
}
