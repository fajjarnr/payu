package id.payu.support.domain.port.in;

import id.payu.support.adapter.persistence.entity.AgentTrainingEntity;

import java.util.List;
import java.util.Optional;
import id.payu.support.domain.CompletionStatus;

/**
 * Inbound port for Agent Training use cases.
 */
public interface AgentTrainingUseCase {

    List<AgentTrainingEntity> getAllAgentTrainings();

    List<AgentTrainingEntity> getTrainingsByAgent(Long agentId);

    List<AgentTrainingEntity> getTrainingsByModule(Long moduleId);

    Optional<AgentTrainingEntity> getAgentTraining(Long agentId, Long moduleId);

    AgentTrainingEntity assignTraining(Long agentId, Long moduleId,
                                 CompletionStatus status,
                                 Integer score, String notes);

    boolean isAgentFullyTrained(Long agentId);

    long countFullyTrainedAgents();
}
