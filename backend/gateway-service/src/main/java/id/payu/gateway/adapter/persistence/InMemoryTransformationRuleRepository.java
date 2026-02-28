package id.payu.gateway.adapter.persistence;

import id.payu.gateway.domain.entity.TransformationRule;
import id.payu.gateway.domain.repository.TransformationRuleRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of TransformationRuleRepository.
 *
 * <p>
 * For production, this should be replaced with a database-backed implementation
 * using PostgreSQL/JPA or configuration files. This implementation provides:
 * - Default transformation rules for common scenarios
 * - In-memory storage for testing and development
 *
 * <p>
 * Note: In a production environment, migrate to JpaTransformationRuleRepository
 * or YamlTransformationRuleRepository.
 */
@ApplicationScoped
public class InMemoryTransformationRuleRepository implements TransformationRuleRepository {

    private final Map<String, TransformationRule> rules = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        createDefaultRules();
        Log.infof("InMemoryTransformationRuleRepository initialized with %d rules", rules.size());
    }

    private void createDefaultRules() {
        // Rule 1: Add security headers to all responses
        TransformationRule securityHeadersRule = new TransformationRule(
            "security-headers",
            "Security Headers",
            "Add security headers to all responses",
            100
        );
        // Add conditions and actions as needed
        rules.put(securityHeadersRule.getId(), securityHeadersRule);

        // Rule 2: Mask sensitive fields for partner APIs
        TransformationRule maskingRule = new TransformationRule(
            "partner-masking",
            "Partner API Masking",
            "Mask sensitive fields in partner API responses",
            200
        );
        rules.put(maskingRule.getId(), maskingRule);

        // Rule 3: Add correlation ID if missing
        TransformationRule correlationIdRule = new TransformationRule(
            "correlation-id",
            "Correlation ID Injection",
            "Add correlation ID header if not present",
            50
        );
        rules.put(correlationIdRule.getId(), correlationIdRule);
    }

    @Override
    public Uni<Optional<TransformationRule>> findById(String id) {
        return Uni.createFrom().item(Optional.ofNullable(rules.get(id)));
    }

    @Override
    public Multi<TransformationRule> findAllActiveOrderedByPriority() {
        List<TransformationRule> sortedRules = rules.values().stream()
            .filter(TransformationRule::isActive)
            .sorted(Comparator.comparingInt(TransformationRule::getPriority))
            .collect(Collectors.toList());

        return Multi.createFrom().iterable(sortedRules);
    }

    @Override
    public Multi<TransformationRule> findAll() {
        return Multi.createFrom().iterable(rules.values());
    }

    @Override
    public Multi<TransformationRule> findApplicableRules(String path, String method) {
        return findAllActiveOrderedByPriority()
            .filter(rule -> {
                TransformationRule.TransformationContext ctx =
                    new TransformationRule.TransformationContext(path, method, Collections.emptyMap(), null);
                return rule.matches(ctx);
            });
    }

    @Override
    public Uni<TransformationRule> save(TransformationRule rule) {
        rules.put(rule.getId(), rule);
        return Uni.createFrom().item(rule);
    }

    @Override
    public Uni<Void> saveBatch(List<TransformationRule> ruleList) {
        for (TransformationRule rule : ruleList) {
            rules.put(rule.getId(), rule);
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Boolean> deleteById(String id) {
        return Uni.createFrom().item(rules.remove(id) != null);
    }

    @Override
    public Uni<Boolean> existsById(String id) {
        return Uni.createFrom().item(rules.containsKey(id));
    }
}
