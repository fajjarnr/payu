package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.LoyaltyPoints;
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
public interface LoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, UUID> {

    List<LoyaltyPoints> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<LoyaltyPoints> findByAccountId(String accountId);

    List<LoyaltyPoints> findByAccountIdAndTransactionId(String accountId, String transactionId);

    /**
     * Find the most recent loyalty points record for an account with pessimistic lock.
     * This prevents race conditions during concurrent balance updates.
     *
     * @param accountId the account ID to lock
     * @return Optional containing the most recent record, or empty if none exists
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lp FROM LoyaltyPoints lp WHERE lp.accountId = :accountId ORDER BY lp.createdAt DESC LIMIT 1")
    Optional<LoyaltyPoints> findTopByAccountIdOrderByCreatedAtDescWithLock(@Param("accountId") String accountId);

    /**
     * Calculate current balance using atomic database sum operation.
     * This is an alternative to locking that avoids the need for balance_after column.
     *
     * @param accountId the account ID
     * @return the calculated balance (null if no records)
     */
    @Query("SELECT SUM(lp.points) FROM LoyaltyPoints lp WHERE lp.accountId = :accountId")
    Integer calculateBalanceByAccountId(@Param("accountId") String accountId);
}
