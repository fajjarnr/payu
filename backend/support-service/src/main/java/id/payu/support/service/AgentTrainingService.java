package id.payu.support.service;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import id.payu.support.domain.TrainingModule;
import id.payu.support.dto.AgentTrainingResponse;
import id.payu.support.dto.AssignTrainingRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AgentTrainingService {

    private static final Logger LOG = Logger.getLogger(AgentTrainingService.class);

    public List<AgentTrainingResponse> getAllAgentTrainings() {
        return AgentTraining.<AgentTraining>listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AgentTrainingResponse> getTrainingsByAgent(Long agentId) {
        return AgentTraining.<AgentTraining>list("agent.id", agentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AgentTrainingResponse> getTrainingsByModule(Long moduleId) {
        return AgentTraining.<AgentTraining>list("trainingModule.id", moduleId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AgentTrainingResponse getAgentTraining(Long agentId, Long moduleId) {
        AgentTraining training = AgentTraining.find("agent.id = ?1 AND trainingModule.id = ?2",
                agentId, moduleId).firstResult();
        return training != null ? toResponse(training) : null;
    }

    @Transactional
    public AgentTrainingResponse assignTraining(AssignTrainingRequest request) {
        LOG.infof("Assigning training: agent=%d, module=%d", request.agentId(), request.trainingModuleId());

        SupportAgent agent = SupportAgent.findById(request.agentId());
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found");
        }

        TrainingModule module = TrainingModule.findById(request.trainingModuleId());
        if (module == null) {
            throw new IllegalArgumentException("Training module not found");
        }

        AgentTraining existing = AgentTraining.<AgentTraining>find("agent.id = ?1 AND trainingModule.id = ?2",
                request.agentId(), request.trainingModuleId()).firstResult();

        AgentTraining agentTraining;
        if (existing != null) {
            agentTraining = existing;
        } else {
            agentTraining = new AgentTraining();
            agentTraining.agent = agent;
            agentTraining.trainingModule = module;
        }

        if (request.status() != null) {
            agentTraining.status = request.status();
            if (request.status() == AgentTraining.CompletionStatus.IN_PROGRESS && agentTraining.startedAt == null) {
                agentTraining.startedAt = LocalDateTime.now();
            }
            if (request.status() == AgentTraining.CompletionStatus.PASSED || 
                request.status() == AgentTraining.CompletionStatus.FAILED) {
                agentTraining.completedAt = LocalDateTime.now();
            }
        }
        agentTraining.score = request.score();
        agentTraining.notes = request.notes();

        agentTraining.persist();
        LOG.infof("Training assigned/updated: id=%d", agentTraining.id);

        return toResponse(agentTraining);
    }

    public boolean isAgentFullyTrained(Long agentId) {
        long mandatoryModules = TrainingModule.count("mandatory = ?1 AND status = ?2",
                true, TrainingModule.TrainingStatus.ACTIVE);

        long completedMandatory = AgentTraining.count(
                "agent.id = ?1 AND trainingModule.mandatory = ?2 AND trainingModule.status = ?3 AND status = ?4",
                agentId, true, TrainingModule.TrainingStatus.ACTIVE, AgentTraining.CompletionStatus.PASSED);

        return completedMandatory >= mandatoryModules;
    }

    public long countFullyTrainedAgents() {
        long totalAgents = SupportAgent.count("active", true);
        long mandatoryModules = TrainingModule.count("mandatory = ?1 AND status = ?2",
                true, TrainingModule.TrainingStatus.ACTIVE);

        if (mandatoryModules == 0) {
            return 0;
        }

        String query = "SELECT COUNT(DISTINCT at.agent.id) FROM AgentTraining at " +
                "WHERE at.agent.active = true " +
                "AND at.trainingModule.mandatory = true " +
                "AND at.trainingModule.status = :moduleStatus " +
                "AND at.status = :completionStatus " +
                "GROUP BY at.agent.id " +
                "HAVING COUNT(at.id) >= :requiredCount";

        return (long) AgentTraining.<AgentTraining>getEntityManager()
                .createQuery(query)
                .setParameter("moduleStatus", TrainingModule.TrainingStatus.ACTIVE)
                .setParameter("completionStatus", AgentTraining.CompletionStatus.PASSED)
                .setParameter("requiredCount", mandatoryModules)
                .getResultStream()
                .count();
    }

    private AgentTrainingResponse toResponse(AgentTraining training) {
        return new AgentTrainingResponse(
                training.id,
                training.agent.id,
                training.agent.name,
                training.trainingModule.id,
                training.trainingModule.title,
                training.status,
                training.score,
                training.startedAt,
                training.completedAt,
                training.notes,
                training.createdAt,
                training.updatedAt
        );
    }
}
