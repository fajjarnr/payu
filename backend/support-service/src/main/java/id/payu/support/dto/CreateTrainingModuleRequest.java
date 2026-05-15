package id.payu.support.dto;

import id.payu.support.domain.TrainingCategory;
import id.payu.support.domain.TrainingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateTrainingModuleRequest(
    @NotBlank String code,
    @NotBlank String title,
    String description,
    TrainingCategory category,
    @Positive int durationMinutes,
    TrainingStatus status,
    boolean mandatory
) {}
