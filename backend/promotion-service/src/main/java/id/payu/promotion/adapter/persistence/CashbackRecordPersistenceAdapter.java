package id.payu.promotion.adapter.persistence;

import id.payu.promotion.domain.model.CashbackRecord;
import id.payu.promotion.domain.port.out.CashbackRecordRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistence adapter for CashbackRecord.
 * Implements the domain port using in-memory storage (for testing/demo).
 * In production, this would use JPA repository.
 */
@Component
public class CashbackRecordPersistenceAdapter implements CashbackRecordRepositoryPort {

    private final Set<String> processedTransactions = ConcurrentHashMap.newKeySet();

    @Override
    public boolean hasProcessedTransaction(String transactionId) {
        return processedTransactions.contains(transactionId);
    }

    @Override
    public CashbackRecord save(CashbackRecord record) {
        processedTransactions.add(record.getTransactionId());
        return record;
    }

    public void clear() {
        processedTransactions.clear();
    }
}
