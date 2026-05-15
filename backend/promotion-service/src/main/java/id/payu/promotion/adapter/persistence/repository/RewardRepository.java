package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.RewardStatus;

@Repository
public interface RewardRepository extends JpaRepository<RewardEntity, UUID> {

    List<RewardEntity> findByAccountId(String accountId);

    List<RewardEntity> findByAccountIdAndStatus(String accountId, RewardStatus status);

    Optional<RewardEntity> findByTransactionId(String transactionId);

    long countByAccountId(String accountId);

    long countByPromotionCode(String promotionCode);
}
