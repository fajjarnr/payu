package id.payu.api.common.exception.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Field-level violation in a ProblemDetail (RFC 9457 §3.1 extension member).
 * Used for bean validation (e.g. {@code @Valid} on a request body).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"field", "message", "rejected_value", "code"})
@Schema(description = "Field-level validation error")
public class FieldViolation {

    @Schema(description = "Name of the field that failed validation",
            example = "amount")
    private String field;

    @Schema(description = "Human-readable error message",
            example = "must be greater than 0")
    private String message;

    @JsonProperty("rejected_value")
    @Schema(description = "The value that was rejected (may be masked for sensitive data)")
    private Object rejectedValue;

    @Schema(description = "Machine-readable error code (e.g. jakarta.validation constraint)")
    private String code;
}
