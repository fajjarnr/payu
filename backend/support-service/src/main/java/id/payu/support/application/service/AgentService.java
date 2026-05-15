package id.payu.support.application.service;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import id.payu.support.dto.AgentResponse;
import id.payu.support.dto.CreateAgentRequest;
import id.payu.support.adapter.persistence.repository.SupportAgentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @CircuitBreaker(name = "support", fallbackMethod = "getAllAgentsFallback")
    @Retry(name = "support")
    public List<AgentResponse> getAllAgents() {
        return agentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CircuitBreaker(name = "support", fallbackMethod = "getAgentByIdFallback")
    @Retry(name = "support")
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

    @CircuitBreaker(name = "support", fallbackMethod = "createAgentFallback")
    @Retry(name = "support")
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

    @CircuitBreaker(name = "support", fallbackMethod = "updateAgentStatusFallback")
    @Retry(name = "support")
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

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private List<AgentResponse> getAllAgentsFallback(Exception ex) {
        log.error("Fallback for getAllAgents: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private AgentResponse getAgentByIdFallback(Long id, Exception ex) {
        log.error("Fallback for getAgentById: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private AgentResponse createAgentFallback(CreateAgentRequest request, Exception ex) {
        log.error("Fallback for createAgent: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private AgentResponse updateAgentStatusFallback(Long id, boolean active, Exception ex) {
        log.error("Fallback for updateAgentStatus: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }
}
