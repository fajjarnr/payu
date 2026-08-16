package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.repository.CashbackRecordRepository;
import id.payu.promotion.domain.model.CashbackRecord;
import id.payu.promotion.domain.model.CashbackStatus;
import id.payu.promotion.domain.port.out.CashbackRecordRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class CashbackRecordPersistenceAdapter implements CashbackRecordRepositoryPort {

    private final CashbackRecordRepository repository;
    private final CashbackRecordPersistenceMapper mapper;

    public CashbackRecordPersistenceAdapter(CashbackRecordRepository repository, CashbackRecordPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean hasProcessedTransaction(String transactionId) {
        return repository.existsByTransactionIdAndStatus(transactionId, CashbackStatus.CREDITED);
    }

    @Override
    public Optional<CashbackRecord> findByTransactionIdAndRuleId(String transactionId, String ruleId) {
        return repository.findByTransactionIdAndRuleId(transactionId, ruleId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CashbackRecord save(CashbackRecord record) {
        return mapper.toDomain(repository.save(mapper.toEntity(record)));
    }

    @Transactional
    public void clear() {
        repository.deleteAllInBatch();
    }
}
