package id.payu.support.dto;

import id.payu.support.domain.CompletionStatus;
import jakarta.validation.constraints.NotNull;

public record AssignTrainingRequest(
    @NotNull Long agentId,
    @NotNull Long moduleId,
    CompletionStatus status,
    Integer score,
    String notes
) {}
