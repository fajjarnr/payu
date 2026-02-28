package id.payu.gateway.adapter.persistence;

import id.payu.gateway.domain.entity.RatePlan;
import id.payu.gateway.domain.repository.RatePlanRepository;
import id.payu.gateway.domain.vo.RateLimit;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of RatePlanRepository.
 *
 * <p>
 * For production, this should be replaced with a database-backed implementation
 * using PostgreSQL/JPA. This implementation provides:
 * - Default rate plans for common scenarios
 * - In-memory storage for testing and development
 *
 * <p>
 * Note: In a production environment, migrate to JpaRatePlanRepository.
 */
@ApplicationScoped
public class InMemoryRatePlanRepository implements RatePlanRepository {

    private final Map<String, RatePlan> plans = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        // Create default rate plans
        createDefaultPlans();
        Log.infof("InMemoryRatePlanRepository initialized with %d plans", plans.size());
    }

    private void createDefaultPlans() {
        // Default plan - moderate limits
        RatePlan defaultPlan = new RatePlan(
            "default",
            "Default Plan",
            "Standard rate limits for most partners",
            RateLimit.of(60, 1000, 10000)
        );
        plans.put(defaultPlan.getId(), defaultPlan);

        // Premium plan - higher limits
        RatePlan premiumPlan = new RatePlan(
            "premium",
            "Premium Plan",
            "Higher rate limits for premium partners",
            RateLimit.of(120, 5000, 50000)
        );
        premiumPlan.addEndpointOverride("/api/v1/transfer", RateLimit.of(30, 500, 5000));
        plans.put(premiumPlan.getId(), premiumPlan);

        // Enterprise plan - highest limits
        RatePlan enterprisePlan = new RatePlan(
            "enterprise",
            "Enterprise Plan",
            "Highest rate limits for enterprise partners",
            RateLimit.of(300, 10000, 100000)
        );
        plans.put(enterprisePlan.getId(), enterprisePlan);

        // Strict plan - lower limits for security
        RatePlan strictPlan = new RatePlan(
            "strict",
            "Strict Plan",
            "Lower rate limits for high-security scenarios",
            RateLimit.of(30, 500, 5000)
        );
        strictPlan.addEndpointOverride("/api/v1/auth/*", RateLimit.of(10, 100, 1000));
        strictPlan.addEndpointOverride("/api/v1/otp/*", RateLimit.of(5, 50, 500));
        plans.put(strictPlan.getId(), strictPlan);
    }

    @Override
    public Uni<Optional<RatePlan>> findById(String id) {
        return Uni.createFrom().item(Optional.ofNullable(plans.get(id)));
    }

    @Override
    public Uni<Optional<RatePlan>> findByName(String name) {
        return Uni.createFrom().item(
            plans.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
        );
    }

    @Override
    public Multi<RatePlan> findAllActive() {
        return Multi.createFrom().iterable(plans.values())
            .filter(RatePlan::isActive);
    }

    @Override
    public Multi<RatePlan> findAll() {
        return Multi.createFrom().iterable(plans.values());
    }

    @Override
    public Uni<RatePlan> save(RatePlan ratePlan) {
        plans.put(ratePlan.getId(), ratePlan);
        return Uni.createFrom().item(ratePlan);
    }

    @Override
    public Uni<Boolean> deleteById(String id) {
        return Uni.createFrom().item(plans.remove(id) != null);
    }

    @Override
    public Uni<Boolean> existsById(String id) {
        return Uni.createFrom().item(plans.containsKey(id));
    }
}
