package id.payu.statement.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating statement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementGenerationRequest {

    @NotNull(message = "User ID is required")
    private java.util.UUID userId;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @Min(value = 2020, message = "Year must be 2020 or later")
    @Max(value = 2099, message = "Year must be 2099 or earlier")
    private Integer year;

    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;
}
