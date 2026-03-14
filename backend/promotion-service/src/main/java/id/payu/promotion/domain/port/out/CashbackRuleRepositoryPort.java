package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.CashbackRule;

import java.util.List;

public interface CashbackRuleRepositoryPort {
    List<CashbackRule> findActiveRules();
    CashbackRule save(CashbackRule rule);
}
