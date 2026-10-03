package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyPointsRepository extends JpaRepository<LoyaltyPointsEntity, UUID> {

    List<LoyaltyPointsEntity> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<LoyaltyPointsEntity> findByAccountId(String accountId);

    List<LoyaltyPointsEntity> findByAccountIdAndTransactionId(String accountId, String transactionId);

    List<LoyaltyPointsEntity> findByAccountIdAndTransactionIdAndTransactionType(
            String accountId, String transactionId, id.payu.promotion.domain.TransactionType transactionType);

    /**
     * Find the most recent loyalty points record for an account with pessimistic lock.
     * This prevents race conditions during concurrent balance updates.
     *
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lp FROM LoyaltyPointsEntity lp WHERE lp.accountId = :accountId ORDER BY lp.createdAt DESC LIMIT 1")
    Optional<LoyaltyPointsEntity> findTopByAccountIdOrderByCreatedAtDescWithLock(@Param("accountId") String accountId);

    /**
     * Calculate current balance using atomic database sum operation.
     * This is an alternative to locking that avoids the need for balance_after column.
     *
     */
    @Query("SELECT SUM(lp.points) FROM LoyaltyPointsEntity lp WHERE lp.accountId = :accountId")
    Integer calculateBalanceByAccountId(@Param("accountId") String accountId);
}
