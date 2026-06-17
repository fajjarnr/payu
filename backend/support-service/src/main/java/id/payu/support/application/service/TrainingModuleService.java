package id.payu.support.application.service;

import id.payu.support.adapter.persistence.entity.TrainingModuleEntity;
import id.payu.support.dto.CreateTrainingModuleRequest;
import id.payu.support.dto.TrainingModuleResponse;
import id.payu.support.adapter.persistence.repository.TrainingModuleRepository;
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

import java.util.List;
import id.payu.support.domain.TrainingStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingModuleService {

    private final TrainingModuleRepository moduleRepository;

    @CircuitBreaker(name = "support", fallbackMethod = "getAllTrainingModulesFallback")
    @Retry(name = "support")
    public List<TrainingModuleResponse> getAllTrainingModules() {
        return moduleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CircuitBreaker(name = "support", fallbackMethod = "getModuleByIdFallback")
    @Retry(name = "support")
    public TrainingModuleResponse getModuleById(Long id) {
        return moduleRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @CircuitBreaker(name = "support", fallbackMethod = "createModuleFallback")
    @Retry(name = "support")
    @Transactional
    public TrainingModuleResponse createModule(CreateTrainingModuleRequest request) {
        log.info("Creating new training module: {} ({})", request.title(), request.code());

        TrainingModuleEntity module = TrainingModuleEntity.builder()
                .code(request.code())
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .durationMinutes(request.durationMinutes())
                .status(request.status() != null ? request.status() : TrainingStatus.DRAFT)
                .mandatory(request.mandatory())
                .build();

        module = moduleRepository.save(module);
        log.info("Training module created: id={}", module.getId());

        return toResponse(module);
    }

    @CircuitBreaker(name = "support", fallbackMethod = "updateModuleStatusFallback")
    @Retry(name = "support")
    @Transactional
    public TrainingModuleResponse updateModuleStatus(Long id, TrainingStatus status) {
        return moduleRepository.findById(id)
                .map(module -> {
                    module.setStatus(status);
                    TrainingModuleEntity updated = moduleRepository.save(module);
                    log.info("Training module {} status updated: {}", id, status);
                    return toResponse(updated);
                })
                .orElse(null);
    }

    public List<TrainingModuleResponse> getMandatoryModules() {
        return moduleRepository.findByStatusAndMandatoryTrue(TrainingStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TrainingModuleResponse toResponse(TrainingModuleEntity module) {
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

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private List<TrainingModuleResponse> getAllTrainingModulesFallback(Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getAllTrainingModules: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private TrainingModuleResponse getModuleByIdFallback(Long id, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getModuleById: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private TrainingModuleResponse createModuleFallback(CreateTrainingModuleRequest request, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for createModule: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }

    private TrainingModuleResponse updateModuleStatusFallback(Long id, TrainingStatus status, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for updateModuleStatus: {}", ex.getMessage());
        throw new RuntimeException("Support service temporarily unavailable", ex);
    }
}
