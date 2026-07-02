package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.SavingsGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.wallet.domain.model.SavingsGoalStatus;

@Repository
public interface SavingsGoalJpaRepository extends JpaRepository<SavingsGoalEntity, UUID> {

    List<SavingsGoalEntity> findByPocketIdAndStatusNot(UUID pocketId, SavingsGoalStatus status);

    List<SavingsGoalEntity> findByUserIdAndStatusNot(UUID userId, SavingsGoalStatus status);

    Optional<SavingsGoalEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByPocketIdAndStatusNot(UUID pocketId, SavingsGoalStatus status);
}
