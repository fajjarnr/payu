package id.payu.abtesting.domain.repository;

import id.payu.abtesting.domain.entity.Experiment;
import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Experiment entities
 */
@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {

    /**
     * Find experiment by unique key
     */
    Optional<Experiment> findByKey(String key);

    /**
     * Find all running experiments
     */
    List<Experiment> findByStatus(ExperimentStatus status);

    /**
     * Find active experiments (running and within date range)
     */
    @Query("SELECT e FROM Experiment e WHERE e.status = 'RUNNING' " +
           "AND (e.startDate IS NULL OR e.startDate <= :currentDate) " +
           "AND (e.endDate IS NULL OR e.endDate >= :currentDate)")
    List<Experiment> findActiveExperiments(@Param("currentDate") LocalDate currentDate);

    /**
     * Find experiments by creator
     */
    List<Experiment> findByCreatedBy(String createdBy);

    /**
     * Find experiments starting within date range
     */
    List<Experiment> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Check if key exists
     */
    boolean existsByKey(String key);
}
