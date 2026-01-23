package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.PocketEntity;
import id.payu.wallet.adapter.persistence.repository.PocketJpaRepository;
import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.port.out.PocketPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PocketPersistenceAdapter implements PocketPersistencePort {

    private final PocketJpaRepository repository;

    public PocketPersistenceAdapter(PocketJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pocket save(Pocket pocket) {
        PocketEntity entity = toEntity(pocket);
        PocketEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Pocket> findById(UUID pocketId) {
        return repository.findById(pocketId).map(this::toDomain);
    }

    @Override
    public List<Pocket> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Pocket> findByAccountIdAndCurrency(String accountId, String currency) {
        return repository.findByAccountIdAndCurrency(accountId, currency).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<Pocket> findAllActive() {
        return repository.findAllActive().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID pocketId) {
        repository.deleteById(pocketId);
    }

    private PocketEntity toEntity(Pocket pocket) {
        PocketEntity entity = new PocketEntity();
        entity.setId(pocket.getId());
        entity.setAccountId(pocket.getAccountId());
        entity.setName(pocket.getName());
        entity.setDescription(pocket.getDescription());
        entity.setCurrency(pocket.getCurrency());
        entity.setBalance(pocket.getBalance());
        entity.setStatus(pocket.getStatus());
        entity.setCreatedAt(pocket.getCreatedAt());
        entity.setUpdatedAt(pocket.getUpdatedAt());
        return entity;
    }

    private Pocket toDomain(PocketEntity entity) {
        return Pocket.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .name(entity.getName())
                .description(entity.getDescription())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
