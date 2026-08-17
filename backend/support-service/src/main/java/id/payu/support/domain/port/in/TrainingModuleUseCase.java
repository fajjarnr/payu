package id.payu.support.domain.port.in;

import id.payu.support.domain.TrainingCategory;
import id.payu.support.domain.TrainingStatus;
import id.payu.support.domain.model.TrainingModule;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port for Training Module use cases.
 */
public interface TrainingModuleUseCase {

    List<TrainingModule> getAllTrainingModules();

    Optional<TrainingModule> getModuleById(Long id);

    TrainingModule createModule(String code, String title, String description,
                                TrainingCategory category,
                                Integer durationMinutes, TrainingStatus status,
                                boolean mandatory);

    TrainingModule updateModuleStatus(Long id, TrainingStatus status);

    List<TrainingModule> getMandatoryModules();
}
