package id.payu.support.application.service;

import id.payu.support.domain.TrainingModule;
import id.payu.support.dto.CreateTrainingModuleRequest;
import id.payu.support.dto.TrainingModuleResponse;
import id.payu.support.adapter.persistence.repository.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingModuleService {

    private final TrainingModuleRepository moduleRepository;

    public List<TrainingModuleResponse> getAllTrainingModules() {
        return moduleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TrainingModuleResponse getModuleById(Long id) {
        return moduleRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public TrainingModuleResponse createModule(CreateTrainingModuleRequest request) {
        log.info("Creating new training module: {} ({})", request.title(), request.code());

        TrainingModule module = TrainingModule.builder()
                .code(request.code())
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .durationMinutes(request.durationMinutes())
                .status(request.status() != null ? request.status() : TrainingModule.TrainingStatus.DRAFT)
                .mandatory(request.mandatory())
                .build();

        module = moduleRepository.save(module);
        log.info("Training module created: id={}", module.getId());

        return toResponse(module);
    }

    @Transactional
    public TrainingModuleResponse updateModuleStatus(Long id, TrainingModule.TrainingStatus status) {
        return moduleRepository.findById(id)
                .map(module -> {
                    module.setStatus(status);
                    TrainingModule updated = moduleRepository.save(module);
                    log.info("Training module {} status updated: {}", id, status);
                    return toResponse(updated);
                })
                .orElse(null);
    }

    public List<TrainingModuleResponse> getMandatoryModules() {
        return moduleRepository.findByStatusAndMandatoryTrue(TrainingModule.TrainingStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TrainingModuleResponse toResponse(TrainingModule module) {
        return new TrainingModuleResponse(
                module.getId(),
                module.getCode(),
                module.getTitle(),
                module.getDescription(),
                module.getCategory(),
                module.getDurationMinutes(),
                module.getStatus(),
                module.isMandatory(),
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }
}
