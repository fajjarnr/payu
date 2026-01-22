package id.payu.support.dto;

import id.payu.support.domain.TrainingModule.TrainingCategory;
import id.payu.support.domain.TrainingModule.TrainingStatus;
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
