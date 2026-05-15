package id.payu.support.domain.port.in;

import id.payu.support.adapter.persistence.entity.SupportAgentEntity;

import java.util.List;
import java.util.Optional;
import id.payu.support.domain.AgentLevel;

/**
 * Inbound port for Support Agent use cases.
 */
public interface AgentUseCase {

    List<SupportAgentEntity> getAllAgents();

    Optional<SupportAgentEntity> getAgentById(Long id);

    Optional<SupportAgentEntity> getAgentByEmployeeId(String employeeId);

    SupportAgentEntity createAgent(String employeeId, String name, String email,
                             String department, AgentLevel level);

    SupportAgentEntity updateAgentStatus(Long id, boolean active);

    long countActiveAgents();

    long countTrainedAgents();
}
