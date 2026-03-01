package id.payu.promotion.adapter.persistence;

import id.payu.promotion.domain.model.CashbackRule;
import id.payu.promotion.domain.port.out.CashbackRuleRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persistence adapter for CashbackRule.
 * Implements the domain port using in-memory storage (for testing/demo).
 * In production, this would use JPA repository.
 */
@Component
public class CashbackRulePersistenceAdapter implements CashbackRuleRepositoryPort {

    private final List<CashbackRule> rules = new CopyOnWriteArrayList<>();

    @Override
    public List<CashbackRule> findActiveRules() {
        return rules.stream()
                .filter(CashbackRule::isActive)
                .toList();
    }

    @Override
    public CashbackRule save(CashbackRule rule) {
        rules.removeIf(r -> r.getRuleId().equals(rule.getRuleId()));
        rules.add(rule);
        return rule;
    }
}
