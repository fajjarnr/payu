package id.payu.gateway.domain.repository;

import id.payu.gateway.domain.entity.PartnerRatePlan;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository port for PartnerRatePlan entities.
 * Follows hexagonal architecture - this is the output port.
 */
public interface PartnerRatePlanRepository {

    /**
     * Find the effective rate plan assignment for a partner.
     * Returns the active assignment that is effective at the given time.
     */
    Uni<Optional<PartnerRatePlan>> findEffectiveByPartnerId(String partnerId, Instant timestamp);

    /**
     * Find the effective rate plan assignment for a partner (now).
     */
    default Uni<Optional<PartnerRatePlan>> findEffectiveByPartnerId(String partnerId) {
        return findEffectiveByPartnerId(partnerId, Instant.now());
    }

    /**
     * Find all assignments for a partner (including historical).
     */
    Multi<PartnerRatePlan> findByPartnerId(String partnerId);

    /**
     * Find all partners assigned to a rate plan.
     */
    Multi<PartnerRatePlan> findByRatePlanId(String ratePlanId);

    /**
     * Save a partner rate plan assignment.
     */
    Uni<PartnerRatePlan> save(PartnerRatePlan partnerRatePlan);

    /**
     * Deactivate all assignments for a partner.
     */
    Uni<Void> deactivateByPartnerId(String partnerId);

    /**
     * Delete an assignment by ID.
     */
    Uni<Boolean> deleteById(String id);
}
