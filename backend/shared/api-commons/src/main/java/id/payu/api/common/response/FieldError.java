package id.payu.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Field-level validation error detail.
 * Used to provide specific error information for request fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Field-level validation error")
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
}
