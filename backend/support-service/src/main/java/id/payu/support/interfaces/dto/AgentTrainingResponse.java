package id.payu.support.interfaces.dto;

import id.payu.support.domain.CompletionStatus;
import java.time.LocalDateTime;

public record AgentTrainingResponse(
    Long id,
    Long agentId,
    String agentName,
    Long trainingModuleId,
    String trainingModuleName,
    CompletionStatus status,
    Integer score,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
