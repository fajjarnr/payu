package id.payu.support.domain.port.in;

import id.payu.support.adapter.persistence.entity.TrainingModuleEntity;

import java.util.List;
import java.util.Optional;
import id.payu.support.domain.TrainingCategory;
import id.payu.support.domain.TrainingStatus;

/**
 * Inbound port for Training Module use cases.
 */
public interface TrainingModuleUseCase {

    List<TrainingModuleEntity> getAllTrainingModules();

    Optional<TrainingModuleEntity> getModuleById(Long id);

    TrainingModuleEntity createModule(String code, String title, String description,
                                TrainingCategory category,
                                Integer durationMinutes, TrainingStatus status,
                                boolean mandatory);

    TrainingModuleEntity updateModuleStatus(Long id, TrainingStatus status);

    List<TrainingModuleEntity> getMandatoryModules();
}
