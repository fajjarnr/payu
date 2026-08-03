package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.CashbackRecordEntity;
import id.payu.promotion.domain.model.CashbackRecord;
import org.springframework.stereotype.Component;

@Component
public class CashbackRecordPersistenceMapper {

    public CashbackRecord toDomain(CashbackRecordEntity entity) {
        CashbackRecord record = new CashbackRecord();
        record.setId(entity.getId().toString());
        record.setTransactionId(entity.getTransactionId());
        record.setAccountId(entity.getAccountId());
        record.setRuleId(entity.getRuleId());
        record.setCashbackAmount(entity.getCashbackAmount());
        record.setStatus(entity.getStatus());
        record.setProcessedAt(entity.getProcessedAt());
        record.setWalletReferenceId(entity.getWalletReferenceId());
        return record;
    }

    public CashbackRecordEntity toEntity(CashbackRecord record) {
        CashbackRecordEntity entity = new CashbackRecordEntity();
        entity.setTransactionId(record.getTransactionId());
        entity.setAccountId(record.getAccountId());
        entity.setRuleId(record.getRuleId());
        entity.setCashbackAmount(record.getCashbackAmount());
        entity.setStatus(record.getStatus());
        entity.setProcessedAt(record.getProcessedAt());
        entity.setWalletReferenceId(record.getWalletReferenceId());
        return entity;
    }
}
