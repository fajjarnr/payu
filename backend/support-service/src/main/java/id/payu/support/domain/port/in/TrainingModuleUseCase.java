package id.payu.support.domain.port.in;

import id.payu.support.domain.TrainingModule;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port for Training Module use cases.
 */
public interface TrainingModuleUseCase {

    List<TrainingModule> getAllTrainingModules();

    Optional<TrainingModule> getModuleById(Long id);

    TrainingModule createModule(String code, String title, String description,
                                TrainingModule.TrainingCategory category,
                                Integer durationMinutes, TrainingModule.TrainingStatus status,
                                boolean mandatory);

    TrainingModule updateModuleStatus(Long id, TrainingModule.TrainingStatus status);

    List<TrainingModule> getMandatoryModules();
}
