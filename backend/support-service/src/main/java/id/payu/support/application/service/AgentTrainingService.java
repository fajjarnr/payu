package id.payu.support.application.service;

import id.payu.support.domain.CompletionStatus;
import id.payu.support.domain.TrainingStatus;
import id.payu.support.domain.model.AgentTraining;
import id.payu.support.domain.model.SupportAgent;
import id.payu.support.domain.model.TrainingModule;
import id.payu.support.domain.port.out.AgentTrainingRepositoryPort;
import id.payu.support.domain.port.out.SupportAgentRepositoryPort;
import id.payu.support.domain.port.out.TrainingModuleRepositoryPort;
import id.payu.support.dto.AgentTrainingResponse;
import id.payu.support.dto.AssignTrainingRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentTrainingService {

    private final AgentTrainingRepositoryPort agentTrainingRepository;
    private final SupportAgentRepositoryPort agentRepository;
    private final TrainingModuleRepositoryPort moduleRepository;

    @CircuitBreaker(name = "support", fallbackMethod = "getAllAgentTrainingsFallback")
    @Retry(name = "support")
    @Transactional(readOnly = true)
    public List<AgentTrainingResponse> getAllAgentTrainings() {
        return agentTrainingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CircuitBreaker(name = "support", fallbackMethod = "getTrainingsByAgentFallback")
    @Retry(name = "support")
    @Transactional(readOnly = true)
    public List<AgentTrainingResponse> getTrainingsByAgent(Long agentId) {
        return agentTrainingRepository.findByAgentId(agentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CircuitBreaker(name = "support", fallbackMethod = "getTrainingsByModuleFallback")
    @Retry(name = "support")
    @Transactional(readOnly = true)
    public List<AgentTrainingResponse> getTrainingsByModule(Long moduleId) {
        return agentTrainingRepository.findByTrainingModuleId(moduleId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentTrainingResponse getAgentTraining(Long agentId, Long moduleId) {
        return agentTrainingRepository.findByAgentIdAndTrainingModuleId(agentId, moduleId)
                .map(this::toResponse)
                .orElse(null);
    }

    @CircuitBreaker(name = "support", fallbackMethod = "assignTrainingFallback")
    @Retry(name = "support")
    @Transactional
    public AgentTrainingResponse assignTraining(AssignTrainingRequest request) {
        log.info("Assigning training: agent={}, module={}", request.agentId(), request.moduleId());

        SupportAgent agent = agentRepository.findById(request.agentId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        TrainingModule module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new IllegalArgumentException("Training module not found"));

        AgentTraining agentTraining = agentTrainingRepository
                .findByAgentIdAndTrainingModuleId(request.agentId(), request.moduleId())
                .orElseGet(() -> AgentTraining.builder()
                        .agentId(agent.getId())
                        .agentName(agent.getName())
                        .trainingModuleId(module.getId())
                        .moduleCode(module.getCode())
                        .moduleTitle(module.getTitle())
                        .build());

        if (request.status() != null) {
            agentTraining.setStatus(request.status());
            if (request.status() == CompletionStatus.IN_PROGRESS && agentTraining.getStartedAt() == null) {
                agentTraining.setStartedAt(LocalDateTime.now());
            }
            if (request.status() == CompletionStatus.PASSED ||
                request.status() == CompletionStatus.FAILED) {
                agentTraining.setCompletedAt(LocalDateTime.now());
            }
        }
        agentTraining.setScore(request.score());
        agentTraining.setNotes(request.notes());

        AgentTraining saved = agentTrainingRepository.save(agentTraining);
        log.info("Training assigned/updated: id={}", saved.getId());

        return toResponse(saved);
    }

    public boolean isAgentFullyTrained(Long agentId) {
        long mandatoryModules = moduleRepository.countByMandatoryTrueAndStatus(TrainingStatus.ACTIVE);
        long completedMandatory = agentTrainingRepository.countByAgentIdAndTrainingModuleMandatoryTrueAndTrainingModuleStatusAndStatus(
                agentId, true, TrainingStatus.ACTIVE, CompletionStatus.PASSED);

        return completedMandatory >= mandatoryModules;
    }

    @Transactional(readOnly = true)
    public long countFullyTrainedAgents() {
        long totalAgents = agentRepository.countByActiveTrue();
        long mandatoryModules = moduleRepository.countByMandatoryTrueAndStatus(TrainingStatus.ACTIVE);

        if (mandatoryModules == 0) {
            return 0;
        }

        // Count agents who have completed all mandatory modules
        return agentRepository.findAll().stream()
                .filter(SupportAgent::isActive)
                .filter(agent -> {
                    long completedCount = agentTrainingRepository
                            .findByAgentId(agent.getId())
                            .stream()
                            .filter(at -> {
                                var mod = moduleRepository.findById(at.getTrainingModuleId());
                                return mod.map(m -> m.isMandatory() && m.getStatus() == TrainingStatus.ACTIVE).orElse(false);
                            })
                            .filter(at -> at.getStatus() == CompletionStatus.PASSED)
                            .count();
                    return completedCount >= mandatoryModules;
                })
                .count();
    }

    private AgentTrainingResponse toResponse(AgentTraining training) {
        return new AgentTrainingResponse(
                training.getId(),
                training.getAgentId(),
                training.getAgentName(),
                training.getTrainingModuleId(),
                training.getModuleTitle(),
                training.getStatus(),
                training.getScore(),
                training.getStartedAt(),
                training.getCompletedAt(),
                training.getNotes(),
                training.getCreatedAt(),
                training.getUpdatedAt()
        );
    }

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private List<AgentTrainingResponse> getAllAgentTrainingsFallback(Exception ex) {
        // L-068: rethrow business exceptions so GlobalExceptionHandler maps them
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getAllAgentTrainings: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private List<AgentTrainingResponse> getTrainingsByAgentFallback(Long agentId, Exception ex) {
        // L-068: rethrow business exceptions so GlobalExceptionHandler maps them
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getTrainingsByAgent: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private List<AgentTrainingResponse> getTrainingsByModuleFallback(Long moduleId, Exception ex) {
        // L-068: rethrow business exceptions so GlobalExceptionHandler maps them
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getTrainingsByModule: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private AgentTrainingResponse assignTrainingFallback(AssignTrainingRequest request, Exception ex) {
        // L-068: rethrow business exceptions so GlobalExceptionHandler maps them
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for assignTraining: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }
}
