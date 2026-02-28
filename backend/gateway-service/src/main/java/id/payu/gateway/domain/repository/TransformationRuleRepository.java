package id.payu.gateway.domain.repository;

import id.payu.gateway.domain.entity.TransformationRule;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for TransformationRule entities.
 * Follows hexagonal architecture - this is the output port.
 */
public interface TransformationRuleRepository {

    /**
     * Find a transformation rule by ID.
     */
    Uni<Optional<TransformationRule>> findById(String id);

    /**
     * Find all active transformation rules ordered by priority.
     */
    Multi<TransformationRule> findAllActiveOrderedByPriority();

    /**
     * Find all transformation rules (including inactive).
     */
    Multi<TransformationRule> findAll();

    /**
     * Find rules applicable to a specific path and method.
     */
    Multi<TransformationRule> findApplicableRules(String path, String method);

    /**
     * Save a transformation rule.
     */
    Uni<TransformationRule> save(TransformationRule rule);

    /**
     * Save multiple rules in batch.
     */
    Uni<Void> saveBatch(List<TransformationRule> rules);

    /**
     * Delete a rule by ID.
     */
    Uni<Boolean> deleteById(String id);

    /**
     * Check if a rule exists by ID.
     */
    Uni<Boolean> existsById(String id);
}
