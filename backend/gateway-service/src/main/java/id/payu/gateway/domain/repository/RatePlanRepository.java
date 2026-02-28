package id.payu.gateway.domain.repository;

import id.payu.gateway.domain.entity.RatePlan;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Optional;

/**
 * Repository port for RatePlan entities.
 * Follows hexagonal architecture - this is the output port.
 */
public interface RatePlanRepository {

    /**
     * Find a rate plan by ID.
     */
    Uni<Optional<RatePlan>> findById(String id);

    /**
     * Find a rate plan by name.
     */
    Uni<Optional<RatePlan>> findByName(String name);

    /**
     * Find all active rate plans.
     */
    Multi<RatePlan> findAllActive();

    /**
     * Find all rate plans (including inactive).
     */
    Multi<RatePlan> findAll();

    /**
     * Save a rate plan.
     */
    Uni<RatePlan> save(RatePlan ratePlan);

    /**
     * Delete a rate plan by ID.
     */
    Uni<Boolean> deleteById(String id);

    /**
     * Check if a rate plan exists by ID.
     */
    Uni<Boolean> existsById(String id);
}
