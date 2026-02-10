package id.payu.support.application.service;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import id.payu.support.dto.AgentResponse;
import id.payu.support.dto.CreateAgentRequest;
import id.payu.support.adapter.persistence.repository.SupportAgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final SupportAgentRepository agentRepository;

    public List<AgentResponse> getAllAgents() {
        return agentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AgentResponse getAgentById(Long id) {
        return agentRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    public AgentResponse getAgentByEmployeeId(String employeeId) {
        return agentRepository.findByEmployeeId(employeeId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request) {
        log.info("Creating new agent: {} ({})", request.name(), request.employeeId());

        SupportAgent agent = SupportAgent.builder()
                .employeeId(request.employeeId())
                .name(request.name())
                .email(request.email())
                .department(request.department())
                .level(request.level() != null ? request.level() : SupportAgent.AgentLevel.JUNIOR)
                .build();

        agent = agentRepository.save(agent);
        log.info("Agent created: id={}", agent.getId());

        return toResponse(agent);
    }

    @Transactional
    public AgentResponse updateAgentStatus(Long id, boolean active) {
        return agentRepository.findById(id)
                .map(agent -> {
                    agent.setActive(active);
                    SupportAgent updated = agentRepository.save(agent);
                    log.info("Agent {} status updated: active={}", id, active);
                    return toResponse(updated);
                })
                .orElse(null);
    }

    public long countActiveAgents() {
        return agentRepository.countByActiveTrue();
    }

    public long countTrainedAgents() {
        return agentRepository.countTrainedAgents();
    }

    private AgentResponse toResponse(SupportAgent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getEmployeeId(),
                agent.getName(),
                agent.getEmail(),
                agent.getDepartment(),
                agent.getLevel(),
                agent.isActive(),
                agent.getCreatedAt(),
                agent.getUpdatedAt()
        );
    }
}
