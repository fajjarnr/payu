package id.payu.support.domain.port.out;

import id.payu.support.domain.TrainingModule;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Training Module persistence.
 */
public interface TrainingModulePersistencePort {

    TrainingModule save(TrainingModule module);

    Optional<TrainingModule> findById(Long id);

    List<TrainingModule> findAll();

    List<TrainingModule> findByStatusAndMandatoryTrue(TrainingModule.TrainingStatus status);

    long countByMandatoryTrueAndStatus(TrainingModule.TrainingStatus status);
}
