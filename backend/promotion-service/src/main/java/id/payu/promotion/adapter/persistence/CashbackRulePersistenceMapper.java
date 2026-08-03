package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.CashbackRuleEntity;
import id.payu.promotion.domain.model.CashbackRule;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;

@Component
public class CashbackRulePersistenceMapper {

    public CashbackRule toDomain(CashbackRuleEntity entity) {
        CashbackRule rule = new CashbackRule();
        rule.setRuleId(entity.getRuleId());
        rule.setName(entity.getName());
        rule.setCashbackType(entity.getCashbackType());
        rule.setCashbackAmount(entity.getCashbackAmount());
        rule.setCashbackPercentage(entity.getCashbackPercentage());
        rule.setMaxCashback(entity.getMaxCashback());
        rule.setMinAmount(entity.getMinAmount());
        rule.setExactAmount(entity.getExactAmount());
        rule.setTieredCashback(entity.getTieredCashback() == null
                ? new HashMap<>() : new HashMap<>(entity.getTieredCashback()));
        rule.setApplicableMerchantCodes(entity.getApplicableMerchantCodes() == null
                ? new HashSet<>() : new HashSet<>(entity.getApplicableMerchantCodes()));
        rule.setApplicableCategories(entity.getApplicableCategories() == null
                ? new HashSet<>() : new HashSet<>(entity.getApplicableCategories()));
        rule.setActive(entity.isActive());
        rule.setValidFrom(entity.getValidFrom());
        rule.setValidUntil(entity.getValidUntil());
        return rule;
    }

    public void updateEntity(CashbackRuleEntity entity, CashbackRule rule) {
        entity.setRuleId(rule.getRuleId());
        entity.setName(rule.getName());
        entity.setCashbackType(rule.getCashbackType());
        entity.setCashbackAmount(rule.getCashbackAmount());
        entity.setCashbackPercentage(rule.getCashbackPercentage());
        entity.setMaxCashback(rule.getMaxCashback());
        entity.setMinAmount(rule.getMinAmount());
        entity.setExactAmount(rule.getExactAmount());
        entity.setTieredCashback(rule.getTieredCashback() == null
                ? new HashMap<>() : new HashMap<>(rule.getTieredCashback()));
        entity.setApplicableMerchantCodes(rule.getApplicableMerchantCodes() == null
                ? new HashSet<>() : new HashSet<>(rule.getApplicableMerchantCodes()));
        entity.setApplicableCategories(rule.getApplicableCategories() == null
                ? new HashSet<>() : new HashSet<>(rule.getApplicableCategories()));
        entity.setActive(rule.isActive());
        entity.setValidFrom(rule.getValidFrom());
        entity.setValidUntil(rule.getValidUntil());
    }

}
