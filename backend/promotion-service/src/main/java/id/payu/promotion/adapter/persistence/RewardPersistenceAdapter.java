package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.repository.RewardRepository;
import id.payu.promotion.domain.model.Reward;
import id.payu.promotion.domain.port.out.RewardPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RewardPersistenceAdapter implements RewardPersistencePort {

    private final RewardRepository repository;
    private final RewardPersistenceMapper mapper;

    public RewardPersistenceAdapter(RewardRepository repository, RewardPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Reward save(Reward reward) {
        return mapper.toDomain(repository.save(mapper.toEntity(reward)));
    }

    @Override
    public Optional<Reward> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Reward> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Reward> findByTransactionId(String transactionId) {
        return repository.findByTransactionId(transactionId).map(mapper::toDomain);
    }
}
