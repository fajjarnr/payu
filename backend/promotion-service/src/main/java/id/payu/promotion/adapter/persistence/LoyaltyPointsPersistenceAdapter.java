package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import id.payu.promotion.domain.model.LoyaltyPoints;
import id.payu.promotion.domain.port.out.LoyaltyPointsRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LoyaltyPointsPersistenceAdapter implements LoyaltyPointsRepositoryPort {
    private final LoyaltyPointsRepository repository;
    private final LoyaltyPointsMapper mapper;
    private final EntityManager entityManager;

    public LoyaltyPointsPersistenceAdapter(LoyaltyPointsRepository repository, LoyaltyPointsMapper mapper,
                                           EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    public LoyaltyPoints save(LoyaltyPoints points) { return mapper.toDomain(repository.save(mapper.toEntity(points))); }
    public Optional<LoyaltyPoints> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public List<LoyaltyPoints> findByAccountIdOrderByCreatedAtDesc(String accountId) {
        return repository.findByAccountIdOrderByCreatedAtDesc(accountId).stream().map(mapper::toDomain).toList();
    }

    public List<LoyaltyPoints> findByAccountIdAndTransactionIdAndTransactionType(
            String accountId, String transactionId, id.payu.promotion.domain.TransactionType transactionType) {
        return repository.findByAccountIdAndTransactionIdAndTransactionType(accountId, transactionId, transactionType)
                .stream().map(mapper::toDomain).toList();
    }
    public Integer calculateBalanceByAccountId(String accountId) { return repository.calculateBalanceByAccountId(accountId); }
    public void lockAccount(String accountId) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:accountId))")
                .setParameter("accountId", accountId).getSingleResult();
    }
    public void flush() { entityManager.flush(); }
}
