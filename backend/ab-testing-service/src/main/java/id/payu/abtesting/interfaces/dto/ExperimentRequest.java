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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public ExperimentStatus getStatus() { return status; }
    public void setStatus(ExperimentStatus status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getTrafficSplit() { return trafficSplit; }
    public void setTrafficSplit(Integer trafficSplit) { this.trafficSplit = trafficSplit; }
    public Map<String, Object> getVariantAConfig() { return variantAConfig; }
    public void setVariantAConfig(Map<String, Object> variantAConfig) { this.variantAConfig = variantAConfig; }
    public Map<String, Object> getVariantBConfig() { return variantBConfig; }
    public void setVariantBConfig(Map<String, Object> variantBConfig) { this.variantBConfig = variantBConfig; }
    public Map<String, Object> getTargetingRules() { return targetingRules; }
    public void setTargetingRules(Map<String, Object> targetingRules) { this.targetingRules = targetingRules; }
}
