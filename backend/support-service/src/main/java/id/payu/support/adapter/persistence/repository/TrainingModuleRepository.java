package id.payu.support.adapter.persistence.repository;

import id.payu.support.domain.TrainingModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingModuleRepository extends JpaRepository<TrainingModule, Long> {

    Optional<TrainingModule> findByCode(String code);

    List<TrainingModule> findByStatus(TrainingModule.TrainingStatus status);

    List<TrainingModule> findByMandatoryTrue();

    List<TrainingModule> findByStatusAndMandatoryTrue(TrainingModule.TrainingStatus status);

    long countByMandatoryTrueAndStatus(TrainingModule.TrainingStatus status);
}
