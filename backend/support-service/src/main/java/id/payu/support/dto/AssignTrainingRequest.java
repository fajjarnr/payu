package id.payu.support.dto;

import id.payu.support.domain.AgentTraining.CompletionStatus;
import jakarta.validation.constraints.NotNull;

public record AssignTrainingRequest(
    @NotNull Long agentId,
    @NotNull Long trainingModuleId,
    CompletionStatus status,
    Integer score,
    String notes
) {}
