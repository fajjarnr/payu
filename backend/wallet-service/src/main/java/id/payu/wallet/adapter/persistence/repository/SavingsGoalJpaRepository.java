package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.SavingsGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavingsGoalJpaRepository extends JpaRepository<SavingsGoalEntity, UUID> {

    List<SavingsGoalEntity> findByPocketIdAndStatusNot(UUID pocketId, SavingsGoalEntity.SavingsGoalStatus status);

    List<SavingsGoalEntity> findByUserIdAndStatusNot(UUID userId, SavingsGoalEntity.SavingsGoalStatus status);

    Optional<SavingsGoalEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByPocketIdAndStatusNot(UUID pocketId, SavingsGoalEntity.SavingsGoalStatus status);
}
