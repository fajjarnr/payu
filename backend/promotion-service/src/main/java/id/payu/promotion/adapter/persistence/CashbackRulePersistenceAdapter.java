package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.CashbackRuleEntity;
import id.payu.promotion.adapter.persistence.repository.CashbackRuleRepository;
import id.payu.promotion.domain.model.CashbackRule;
import id.payu.promotion.domain.port.out.CashbackRuleRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CashbackRulePersistenceAdapter implements CashbackRuleRepositoryPort {

    private final CashbackRuleRepository repository;
    private final CashbackRulePersistenceMapper mapper;

    public CashbackRulePersistenceAdapter(CashbackRuleRepository repository, CashbackRulePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CashbackRule> findActiveRules() {
        return repository.findByActiveTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public CashbackRule save(CashbackRule rule) {
        CashbackRuleEntity entity = repository.findByRuleId(rule.getRuleId()).orElseGet(CashbackRuleEntity::new);
        mapper.updateEntity(entity, rule);
        return mapper.toDomain(repository.save(entity));
    }

    @Transactional
    public void clear() {
        repository.deleteAllInBatch();
    }
}
