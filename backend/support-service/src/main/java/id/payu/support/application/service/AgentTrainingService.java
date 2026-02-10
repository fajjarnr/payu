package id.payu.support.application.service;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import id.payu.support.domain.TrainingModule;
import id.payu.support.dto.AgentTrainingResponse;
import id.payu.support.dto.AssignTrainingRequest;
import id.payu.support.adapter.persistence.repository.AgentTrainingRepository;
import id.payu.support.adapter.persistence.repository.SupportAgentRepository;
import id.payu.support.adapter.persistence.repository.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentTrainingService {

    private final AgentTrainingRepository agentTrainingRepository;
    private final SupportAgentRepository agentRepository;
    private final TrainingModuleRepository moduleRepository;

    public List<AgentTrainingResponse> getAllAgentTrainings() {
        return agentTrainingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AgentTrainingResponse> getTrainingsByAgent(Long agentId) {
        return agentTrainingRepository.findByAgentId(agentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AgentTrainingResponse> getTrainingsByModule(Long moduleId) {
        return agentTrainingRepository.findByTrainingModuleId(moduleId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AgentTrainingResponse getAgentTraining(Long agentId, Long moduleId) {
        return agentTrainingRepository.findByAgentIdAndTrainingModuleId(agentId, moduleId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public AgentTrainingResponse assignTraining(AssignTrainingRequest request) {
        log.info("Assigning training: agent={}, module={}", request.agentId(), request.moduleId());

        SupportAgent agent = agentRepository.findById(request.agentId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        TrainingModule module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new IllegalArgumentException("Training module not found"));

        AgentTraining agentTraining = agentTrainingRepository
                .findByAgentAndTrainingModule(agent, module)
                .orElseGet(() -> AgentTraining.builder()
                        .agent(agent)
                        .trainingModule(module)
                        .build());

        if (request.status() != null) {
            agentTraining.setStatus(request.status());
            if (request.status() == AgentTraining.CompletionStatus.IN_PROGRESS && agentTraining.getStartedAt() == null) {
                agentTraining.setStartedAt(LocalDateTime.now());
            }
            if (request.status() == AgentTraining.CompletionStatus.PASSED ||
                request.status() == AgentTraining.CompletionStatus.FAILED) {
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
        long mandatoryModules = moduleRepository.countByMandatoryTrueAndStatus(TrainingModule.TrainingStatus.ACTIVE);
        long completedMandatory = agentTrainingRepository.countByAgentIdAndTrainingModuleMandatoryTrueAndTrainingModuleStatusAndStatus(
                agentId, true, TrainingModule.TrainingStatus.ACTIVE, AgentTraining.CompletionStatus.PASSED);

        return completedMandatory >= mandatoryModules;
    }

    public long countFullyTrainedAgents() {
        long totalAgents = agentRepository.countByActiveTrue();
        long mandatoryModules = moduleRepository.countByMandatoryTrueAndStatus(TrainingModule.TrainingStatus.ACTIVE);

        if (mandatoryModules == 0) {
            return 0;
        }

        // Count agents who have completed all mandatory modules
        return agentRepository.findAll().stream()
                .filter(SupportAgent::isActive)
                .filter(agent -> {
                    long completedCount = agentTrainingRepository
                            .findByAgent(agent)
                            .stream()
                            .filter(at -> at.getTrainingModule().isMandatory())
                            .filter(at -> at.getTrainingModule().getStatus() == TrainingModule.TrainingStatus.ACTIVE)
                            .filter(at -> at.getStatus() == AgentTraining.CompletionStatus.PASSED)
                            .count();
                    return completedCount >= mandatoryModules;
                })
                .count();
    }

    private AgentTrainingResponse toResponse(AgentTraining training) {
        return new AgentTrainingResponse(
                training.getId(),
                training.getAgent().getId(),
                training.getAgent().getName(),
                training.getTrainingModule().getId(),
                training.getTrainingModule().getTitle(),
                training.getStatus(),
                training.getScore(),
                training.getStartedAt(),
                training.getCompletedAt(),
                training.getNotes(),
                training.getCreatedAt(),
                training.getUpdatedAt()
        );
    }
}
