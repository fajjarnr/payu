package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.CashbackRecord;

import java.util.Optional;

public interface CashbackRecordRepositoryPort {
    boolean hasProcessedTransaction(String transactionId);
    Optional<CashbackRecord> findByTransactionIdAndRuleId(String transactionId, String ruleId);
    CashbackRecord save(CashbackRecord record);
}
