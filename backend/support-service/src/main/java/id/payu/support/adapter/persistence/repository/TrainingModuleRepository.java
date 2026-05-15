package id.payu.support.adapter.persistence.repository;

import id.payu.support.adapter.persistence.entity.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import id.payu.support.domain.TrainingStatus;

public interface TrainingModuleRepository extends JpaRepository<TrainingModuleEntity, Long> {

    Optional<TrainingModuleEntity> findByCode(String code);

    List<TrainingModuleEntity> findByStatus(TrainingStatus status);

    List<TrainingModuleEntity> findByMandatoryTrue();

    List<TrainingModuleEntity> findByStatusAndMandatoryTrue(TrainingStatus status);

    long countByMandatoryTrueAndStatus(TrainingStatus status);
}
