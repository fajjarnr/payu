package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.entity.AggregateResultEntity;
import id.payu.transaction.adapter.persistence.repository.AggregateResultJpaRepository;
import id.payu.transaction.domain.port.out.AggregateResultPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AggregateResultPersistenceAdapter implements AggregateResultPort {
    private final AggregateResultJpaRepository repo;

    public AggregateResultPersistenceAdapter(AggregateResultJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResult(String referenceNo, String resultJson, int fanoutOrder) {
        AggregateResultEntity e = new AggregateResultEntity(UUID.randomUUID(), referenceNo, resultJson, fanoutOrder, Instant.now());
        repo.save(e);
    }

    @Override
    public List<String> findByReferenceNo(String referenceNo) {
        return repo.findByReferenceNo(referenceNo).stream().map(AggregateResultEntity::getResult).collect(Collectors.toList());
    }
}
