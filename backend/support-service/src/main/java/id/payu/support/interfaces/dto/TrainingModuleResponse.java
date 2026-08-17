package id.payu.support.interfaces.dto;

import id.payu.support.domain.TrainingCategory;
import id.payu.support.domain.TrainingStatus;
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
