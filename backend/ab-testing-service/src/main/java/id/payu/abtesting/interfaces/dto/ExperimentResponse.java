package id.payu.abtesting.interfaces.dto;

import id.payu.abtesting.domain.entity.Experiment;
import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for experiment responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentResponse {

    private UUID id;
    private String name;
    private String description;
    private String key;
    private ExperimentStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer trafficSplit;
    private Map<String, Object> variantAConfig;
    private Map<String, Object> variantBConfig;
    private Map<String, Object> targetingRules;
    private Map<String, Object> metrics;
    private Double confidenceLevel;
    private String winner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    /**
     * Convert entity to DTO
     */
    /**
     * Convert entity to DTO
     */
    public static ExperimentResponse fromEntity(Experiment experiment) {
        ExperimentResponse response = new ExperimentResponse();
        response.setId(experiment.getId());
        response.setName(experiment.getName());
        response.setDescription(experiment.getDescription());
        response.setKey(experiment.getKey());
        response.setStatus(experiment.getStatus());
        response.setStartDate(experiment.getStartDate());
        response.setEndDate(experiment.getEndDate());
        response.setTrafficSplit(experiment.getTrafficSplit());
        response.setVariantAConfig(experiment.getVariantAConfig());
        response.setVariantBConfig(experiment.getVariantBConfig());
        response.setTargetingRules(experiment.getTargetingRules());
        response.setMetrics(experiment.getMetrics());
        response.setConfidenceLevel(experiment.getConfidenceLevel());
        response.setWinner(experiment.getWinner());
        response.setCreatedAt(experiment.getCreatedAt());
        response.setUpdatedAt(experiment.getUpdatedAt());
        response.setCreatedBy(experiment.getCreatedBy());
        return response;
    }

    // Manual accessors to bypass Lombok issues
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    public Double getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(Double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
