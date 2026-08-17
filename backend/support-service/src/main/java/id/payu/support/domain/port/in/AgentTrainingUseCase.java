package id.payu.support.domain.port.in;

import id.payu.support.domain.CompletionStatus;
import id.payu.support.domain.model.AgentTraining;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port for Agent Training use cases.
 */
public interface AgentTrainingUseCase {

    List<AgentTraining> getAllAgentTrainings();

    List<AgentTraining> getTrainingsByAgent(Long agentId);

    List<AgentTraining> getTrainingsByModule(Long moduleId);

    Optional<AgentTraining> getAgentTraining(Long agentId, Long moduleId);

    AgentTraining assignTraining(Long agentId, Long moduleId,
                                 CompletionStatus status,
                                 Integer score, String notes);

    boolean isAgentFullyTrained(Long agentId);

    long countFullyTrainedAgents();
}
