package id.payu.support.service;

import id.payu.support.domain.TrainingModule;
import id.payu.support.dto.CreateTrainingModuleRequest;
import id.payu.support.dto.TrainingModuleResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class TrainingModuleService {

    private static final Logger LOG = Logger.getLogger(TrainingModuleService.class);

    public List<TrainingModuleResponse> getAllTrainingModules() {
        return TrainingModule.<TrainingModule>listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public TrainingModuleResponse getModuleById(Long id) {
        TrainingModule module = TrainingModule.findById(id);
        return module != null ? toResponse(module) : null;
    }

    @Transactional
    public TrainingModuleResponse createModule(CreateTrainingModuleRequest request) {
        LOG.infof("Creating new training module: %s (%s)", request.title(), request.code());

        TrainingModule module = new TrainingModule();
        module.code = request.code();
        module.title = request.title();
        module.description = request.description();
        module.category = request.category();
        module.durationMinutes = request.durationMinutes();
        module.status = request.status() != null ? request.status() : TrainingModule.TrainingStatus.DRAFT;
        module.mandatory = request.mandatory();

        module.persist();
        LOG.infof("Training module created: id=%d", module.id);

        return toResponse(module);
    }

    @Transactional
    public TrainingModuleResponse updateModuleStatus(Long id, TrainingModule.TrainingStatus status) {
        TrainingModule module = TrainingModule.findById(id);
        if (module == null) {
            return null;
        }

        module.status = status;
        module.persist();
        LOG.infof("Training module %d status updated: %s", id, status);

        return toResponse(module);
    }

    public List<TrainingModuleResponse> getMandatoryModules() {
        return TrainingModule.<TrainingModule>list("mandatory = ?1 AND status = ?2",
                true, TrainingModule.TrainingStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TrainingModuleResponse toResponse(TrainingModule module) {
        return new TrainingModuleResponse(
                module.id,
                module.code,
                module.title,
                module.description,
                module.category,
                module.durationMinutes,
                module.status,
                module.mandatory,
                module.createdAt,
                module.updatedAt
        );
    }
}
