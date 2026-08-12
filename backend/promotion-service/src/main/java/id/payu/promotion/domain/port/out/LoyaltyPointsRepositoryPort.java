package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.TransactionType;
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

    /**
     * PROMO-002 (CB-027): redemption dedup guard for a given transaction.
     */
    List<LoyaltyPoints> findByAccountIdAndTransactionIdAndTransactionType(
            String accountId, String transactionId, TransactionType transactionType);
}
