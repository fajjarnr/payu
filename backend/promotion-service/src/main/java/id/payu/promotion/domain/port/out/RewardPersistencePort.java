package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.Reward;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardPersistencePort {
    Reward save(Reward reward);

    Optional<Reward> findById(UUID id);

    List<Reward> findByAccountId(String accountId);

    /**
     * PROMO-003 (CB-032): duplicate claim guard for a given transaction.
     */
    Optional<Reward> findByTransactionId(String transactionId);
}
