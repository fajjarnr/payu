package id.payu.support.domain.port.out;

import id.payu.support.domain.TrainingStatus;
import id.payu.support.domain.model.TrainingModule;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for TrainingModule persistence.
 */
public interface TrainingModuleRepositoryPort {

    List<TrainingModule> findAll();

    Optional<TrainingModule> findById(Long id);

    Optional<TrainingModule> findByCode(String code);

    TrainingModule save(TrainingModule module);

    List<TrainingModule> findByStatus(TrainingStatus status);

    List<TrainingModule> findByMandatoryTrue();

    List<TrainingModule> findByStatusAndMandatoryTrue(TrainingStatus status);

    long countByMandatoryTrueAndStatus(TrainingStatus status);
}
