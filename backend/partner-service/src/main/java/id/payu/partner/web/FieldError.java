package id.payu.partner.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Field-level validation error detail.
 * Used to provide specific error information for request fields.
 */
@Schema(description = "Field-level validation error")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldError {

    @Schema(
        description = "Field name that failed validation",
        example = "amount"
    )
    private String field;

    @Schema(
        description = "Validation error message for the field",
        example = "Jumlah melebihi saldo tersedia (Rp 500.000)"
    )
    private String message;

    @Schema(
        description = "Rejected value (optional)",
        example = "1500000"
    )
    private Object rejectedValue;

    public FieldError() {}

    public FieldError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public FieldError(String field, String message, Object rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getRejectedValue() { return rejectedValue; }
    public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }

    public static FieldError of(String field, String message) {
        return new FieldError(field, message);
    }

    public static FieldError of(String field, String message, Object rejectedValue) {
        return new FieldError(field, message, rejectedValue);
    }
}
