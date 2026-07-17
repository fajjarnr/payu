package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.LoyaltyPoints;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyPointsRepositoryPort {
    LoyaltyPoints save(LoyaltyPoints points);
    Optional<LoyaltyPoints> findById(UUID id);
    List<LoyaltyPoints> findByAccountIdOrderByCreatedAtDesc(String accountId);
    Integer calculateBalanceByAccountId(String accountId);
    void lockAccount(String accountId);
    void flush();
}
