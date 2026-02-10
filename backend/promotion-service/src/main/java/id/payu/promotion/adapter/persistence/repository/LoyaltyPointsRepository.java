package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.LoyaltyPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, UUID> {

    List<LoyaltyPoints> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<LoyaltyPoints> findByAccountId(String accountId);

    List<LoyaltyPoints> findByAccountIdAndTransactionId(String accountId, String transactionId);
}
