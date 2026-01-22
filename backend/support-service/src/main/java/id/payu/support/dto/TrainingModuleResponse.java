package id.payu.support.dto;

import id.payu.support.domain.TrainingModule.TrainingCategory;
import id.payu.support.domain.TrainingModule.TrainingStatus;
import java.time.LocalDateTime;

public record TrainingModuleResponse(
    Long id,
    String code,
    String title,
    String description,
    TrainingCategory category,
    int durationMinutes,
    TrainingStatus status,
    boolean mandatory,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
