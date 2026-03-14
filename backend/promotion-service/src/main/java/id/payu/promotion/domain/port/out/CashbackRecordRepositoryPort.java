package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.CashbackRecord;

public interface CashbackRecordRepositoryPort {
    boolean hasProcessedTransaction(String transactionId);
    CashbackRecord save(CashbackRecord record);
}
