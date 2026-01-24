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
    public static ExperimentResponse fromEntity(Experiment experiment) {
        return ExperimentResponse.builder()
                .id(experiment.getId())
                .name(experiment.getName())
                .description(experiment.getDescription())
                .key(experiment.getKey())
                .status(experiment.getStatus())
                .startDate(experiment.getStartDate())
                .endDate(experiment.getEndDate())
                .trafficSplit(experiment.getTrafficSplit())
                .variantAConfig(experiment.getVariantAConfig())
                .variantBConfig(experiment.getVariantBConfig())
                .targetingRules(experiment.getTargetingRules())
                .metrics(experiment.getMetrics())
                .confidenceLevel(experiment.getConfidenceLevel())
                .winner(experiment.getWinner())
                .createdAt(experiment.getCreatedAt())
                .updatedAt(experiment.getUpdatedAt())
                .createdBy(experiment.getCreatedBy())
                .build();
    }
}
