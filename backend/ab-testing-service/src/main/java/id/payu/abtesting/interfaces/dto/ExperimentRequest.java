package id.payu.abtesting.interfaces.dto;

import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for creating/updating experiments
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Key is required")
    private String key;

    private ExperimentStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Traffic split is required")
    @Min(value = 0, message = "Traffic split must be at least 0")
    @Max(value = 100, message = "Traffic split must be at most 100")
    private Integer trafficSplit;

    private Map<String, Object> variantAConfig;

    private Map<String, Object> variantBConfig;

    private Map<String, Object> targetingRules;
}
