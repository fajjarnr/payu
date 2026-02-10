package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RewardRepository extends JpaRepository<Reward, UUID> {

    List<Reward> findByAccountId(String accountId);

    List<Reward> findByAccountIdAndStatus(String accountId, Reward.Status status);

    Optional<Reward> findByTransactionId(String transactionId);
}
