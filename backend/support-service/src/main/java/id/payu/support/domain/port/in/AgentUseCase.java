package id.payu.support.domain.port.in;

import id.payu.support.domain.AgentLevel;
import id.payu.support.domain.model.SupportAgent;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port for Support Agent use cases.
 */
public interface AgentUseCase {

    List<SupportAgent> getAllAgents();

    Optional<SupportAgent> getAgentById(Long id);

    Optional<SupportAgent> getAgentByEmployeeId(String employeeId);

    SupportAgent createAgent(String employeeId, String name, String email,
                             String department, AgentLevel level);

    SupportAgent updateAgentStatus(Long id, boolean active);

    long countActiveAgents();

    long countTrainedAgents();
}
