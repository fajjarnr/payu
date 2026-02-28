package id.payu.gateway.adapter.persistence;

import id.payu.gateway.domain.entity.PartnerRatePlan;
import id.payu.gateway.domain.repository.PartnerRatePlanRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of PartnerRatePlanRepository.
 *
 * <p>
 * For production, this should be replaced with a database-backed implementation
 * using PostgreSQL/JPA. This implementation provides:
 * - In-memory storage for testing and development
 * - Automatic assignment of default plan to known partners
 *
 * <p>
 * Note: In a production environment, migrate to JpaPartnerRatePlanRepository.
 */
@ApplicationScoped
public class InMemoryPartnerRatePlanRepository implements PartnerRatePlanRepository {

    private final Map<String, PartnerRatePlan> assignments = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        // Assign default plan to some example partners
        assignDefaultPlan("tokobapak", "premium");
        assignDefaultPlan("nobar", "enterprise");
        assignDefaultPlan("demo-partner", "default");

        Log.infof("InMemoryPartnerRatePlanRepository initialized with %d assignments", assignments.size());
    }

    private void assignDefaultPlan(String partnerId, String planId) {
        PartnerRatePlan assignment = new PartnerRatePlan(
            partnerId + "-" + planId,
            partnerId,
            planId
        );
        assignments.put(assignment.getId(), assignment);
    }

    @Override
    public Uni<Optional<PartnerRatePlan>> findEffectiveByPartnerId(String partnerId, Instant timestamp) {
        return Uni.createFrom().item(
            assignments.values().stream()
                .filter(a -> a.getPartnerId().equals(partnerId))
                .filter(a -> a.isEffectiveAt(timestamp))
                .findFirst()
        );
    }

    @Override
    public Multi<PartnerRatePlan> findByPartnerId(String partnerId) {
        return Multi.createFrom().iterable(
            assignments.values().stream()
                .filter(a -> a.getPartnerId().equals(partnerId))
                .collect(Collectors.toList())
        );
    }

    @Override
    public Multi<PartnerRatePlan> findByRatePlanId(String ratePlanId) {
        return Multi.createFrom().iterable(
            assignments.values().stream()
                .filter(a -> a.getRatePlanId().equals(ratePlanId))
                .collect(Collectors.toList())
        );
    }

    @Override
    public Uni<PartnerRatePlan> save(PartnerRatePlan partnerRatePlan) {
        assignments.put(partnerRatePlan.getId(), partnerRatePlan);
        return Uni.createFrom().item(partnerRatePlan);
    }

    @Override
    public Uni<Void> deactivateByPartnerId(String partnerId) {
        assignments.values().stream()
            .filter(a -> a.getPartnerId().equals(partnerId))
            .forEach(PartnerRatePlan::deactivate);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Boolean> deleteById(String id) {
        return Uni.createFrom().item(assignments.remove(id) != null);
    }
}
