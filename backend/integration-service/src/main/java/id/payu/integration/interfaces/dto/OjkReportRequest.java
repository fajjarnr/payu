package id.payu.integration.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for OJK report generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OJK report generation request")
public class OjkReportRequest {

    @NotBlank(message = "Report type is required")
    @Schema(description = "Report type", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "DAILY_CSV", allowableValues = {"DAILY_CSV", "DAILY_XML", "MONTHLY_CSV", "MONTHLY_XML"})
    private String reportType;

    @NotNull(message = "Report date is required")
    @Schema(description = "Report date", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    private LocalDate reportDate;

    @Schema(description = "Institution code (defaults to PAYU)", example = "PAYU")
    private String institutionCode;
}
