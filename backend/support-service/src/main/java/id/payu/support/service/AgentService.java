package id.payu.support.service;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import id.payu.support.dto.AgentResponse;
import id.payu.support.dto.CreateAgentRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class AgentService {

    private static final Logger LOG = Logger.getLogger(AgentService.class);

    public List<AgentResponse> getAllAgents() {
        return SupportAgent.<SupportAgent>listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AgentResponse getAgentById(Long id) {
        SupportAgent agent = SupportAgent.findById(id);
        return agent != null ? toResponse(agent) : null;
    }

    public AgentResponse getAgentByEmployeeId(String employeeId) {
        SupportAgent agent = SupportAgent.find("employeeId", employeeId).firstResult();
        return agent != null ? toResponse(agent) : null;
    }

    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request) {
        LOG.infof("Creating new agent: %s (%s)", request.name(), request.employeeId());

        SupportAgent agent = new SupportAgent();
        agent.employeeId = request.employeeId();
        agent.name = request.name();
        agent.email = request.email();
        agent.department = request.department();
        agent.level = request.level() != null ? request.level() : SupportAgent.AgentLevel.JUNIOR;

        agent.persist();
        LOG.infof("Agent created: id=%d", agent.id);

        return toResponse(agent);
    }

    @Transactional
    public AgentResponse updateAgentStatus(Long id, boolean active) {
        SupportAgent agent = SupportAgent.findById(id);
        if (agent == null) {
            return null;
        }

        agent.active = active;
        agent.persist();
        LOG.infof("Agent %d status updated: active=%s", id, active);

        return toResponse(agent);
    }

    public long countActiveAgents() {
        return SupportAgent.count("active", true);
    }

    public long countTrainedAgents() {
        return SupportAgent.count("active = ?1 AND id IN (SELECT at.agent.id FROM AgentTraining at WHERE at.status = ?2)", 
                true, AgentTraining.CompletionStatus.PASSED);
    }

    private AgentResponse toResponse(SupportAgent agent) {
        return new AgentResponse(
                agent.id,
                agent.employeeId,
                agent.name,
                agent.email,
                agent.department,
                agent.level,
                agent.active,
                agent.createdAt,
                agent.updatedAt
        );
    }
}
