package id.payu.support.domain.port.out;

import id.payu.support.adapter.persistence.entity.TrainingModuleEntity;

import java.util.List;
import java.util.Optional;
import id.payu.support.domain.TrainingStatus;

/**
 * Outbound port for Training Module persistence.
 */
public interface TrainingModulePersistencePort {

    TrainingModuleEntity save(TrainingModuleEntity module);

    Optional<TrainingModuleEntity> findById(Long id);

    List<TrainingModuleEntity> findAll();

    List<TrainingModuleEntity> findByStatusAndMandatoryTrue(TrainingStatus status);

    long countByMandatoryTrueAndStatus(TrainingStatus status);
}
